package hk.whatoeat.domain

import hk.whatoeat.core.model.*
import hk.whatoeat.domain.draw.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class DrawEngineTest {
    private var calls=0
    private var bound=0
    private val time=TimeSource { 42 }
    private val engine=DrawEngine(RandomSource { n -> calls++;bound=n;n-1 },time,IdSource { "draw" })
    private fun draw(ids: List<String>,drawn: Set<String> = emptySet(),mode: Presentation=Presentation.Direct) = engine.prepare(ids,drawn,"v",mode,"summary")
    @Test fun emptyDoesNotCallRandom() {assertEquals(DrawOutcome.Empty,draw(emptyList()));assertEquals(0,calls)}
    @Test fun singleDoesNotCallRandom() {assertEquals("a",(draw(listOf("a")) as DrawOutcome.Selected).session.winnerId);assertEquals(0,calls)}
    @Test fun stableOrderingDedupAndOneUniformChoice() {
        val s=(draw(listOf("c","b","a","a")) as DrawOutcome.Selected).session
        assertEquals(listOf("a","b","c"),s.orderedCandidateIds);assertEquals("c",s.winnerId);assertEquals(2,s.winnerIndex)
        assertEquals(1,calls);assertEquals(3,bound)
    }
    @Test fun sameRandomInputSameWinnerForBothModes() {
        val direct=(draw(listOf("b","a","c")) as DrawOutcome.Selected).session
        val wheel=(draw(listOf("b","a","c"),mode=Presentation.Wheel) as DrawOutcome.Selected).session
        assertEquals(direct.winnerId,wheel.winnerId);assertEquals(direct.orderedCandidateIds,wheel.orderedCandidateIds)
    }
    @Test fun removesDrawnBeforeChoosingAndReportsExhaustion() {
        assertEquals("b",(draw(listOf("a","b"),setOf("a")) as DrawOutcome.Selected).session.winnerId)
        assertEquals(0,calls);assertEquals(DrawOutcome.Exhausted,draw(listOf("a","b"),setOf("a","b")));assertEquals(0,calls)
    }
    @Test fun rapidTapsAndRepeatedCompletionPreserveWinner() = runTest {
        val store=MemoryRoundStore()
        val c=DrawCoordinator(store,engine,time)
        val outcomes=(1..20).map { async { c.prepare(listOf("a","b"),"v",Presentation.Wheel,"f") } }.awaitAll()
        assertEquals(1,calls);assertEquals(1,outcomes.distinct().size)
        (1..20).map { async { c.reveal("draw") } }.awaitAll()
        assertEquals(1,store.history.size);assertEquals(setOf("b"),store.round.drawn)
    }
    @Test fun restoredPreparedSessionReusesWinnerWithoutRandomness() = runTest {
        val store=MemoryRoundStore();val c=DrawCoordinator(store,engine,time)
        val first=c.prepare(listOf("a","b"),"v",Presentation.Wheel,"f")
        val restored=DrawCoordinator(store,engine,time)
        assertEquals(first,restored.prepare(listOf("x"),"other",Presentation.Direct,"f"));assertEquals(1,calls)
        restored.reveal("draw");assertEquals(1,store.history.size)
    }
    @Test fun explicitRestartOnlyAndModeDoesNotResetRound() = runTest {
        val store=MemoryRoundStore();val c=DrawCoordinator(store,engine,time)
        c.prepare(listOf("a"),"v",Presentation.Direct,"f");c.reveal("draw");c.dismiss()
        assertEquals(DrawOutcome.Exhausted,c.prepare(listOf("a"),"v",Presentation.Wheel,"f"))
        c.restart("v");assertTrue(c.prepare(listOf("a"),"v",Presentation.Wheel,"f") is DrawOutcome.Selected)
    }
    @Test fun failedPreparationNeverRevealsOrWritesHistory() = runTest {
        val store=MemoryRoundStore();store.failSave=true;val c=DrawCoordinator(store,engine,time)
        try { c.prepare(listOf("a","b"),"v",Presentation.Direct,"f");fail() } catch(_: IllegalStateException) { }
        assertNull(store.round.session);assertTrue(store.history.isEmpty())
        store.failSave=false
        val recovered=c.prepare(listOf("a","b"),"v",Presentation.Direct,"f") as DrawOutcome.Selected
        assertEquals(1,calls)
        c.reveal(recovered.session.drawId);assertEquals(1,store.history.size)
    }
    @Test fun failureDuringRevealPreservesPreparedSession() = runTest {
        val store=MemoryRoundStore();val c=DrawCoordinator(store,engine,time)
        c.prepare(listOf("a"),"v",Presentation.Direct,"f");store.failReveal=true
        try {c.reveal("draw");fail()} catch(_: IllegalStateException) { }
        assertEquals(SessionStatus.Prepared,store.round.session?.status);assertTrue(store.history.isEmpty())
        store.failReveal=false;c.reveal("draw");assertEquals(SessionStatus.Revealed,store.round.session?.status)
    }
}
internal class MemoryRoundStore : RoundStore {
    var round=Round();val history=mutableSetOf<String>();var failSave=false;var failReveal=false
    override suspend fun read()=round
    override suspend fun save(round: Round) {check(!failSave);this.round=round}
    override suspend fun reveal(drawId: String,now: Long): Round {
        check(!failReveal)
        val s=round.session
        if(s!=null && s.drawId==drawId && s.status==SessionStatus.Prepared) {
            history+=drawId;round=round.copy(drawn=round.drawn+s.winnerId,session=s.copy(status=SessionStatus.Revealed))
        }
        return round
    }
    override suspend fun clearAll() {round=Round();history.clear()}
}
