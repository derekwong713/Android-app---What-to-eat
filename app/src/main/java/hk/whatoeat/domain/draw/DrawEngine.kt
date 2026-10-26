package hk.whatoeat.domain.draw

import hk.whatoeat.core.model.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface DrawOutcome {
    data object Empty : DrawOutcome
    data object Exhausted : DrawOutcome
    data class Selected(val session: DrawSession) : DrawOutcome
}
class DrawEngine(private val random: RandomSource, private val time: TimeSource, private val ids: IdSource) {
    fun prepare(candidateIds: List<String>, drawn: Set<String>, version: String, mode: Presentation, summary: String): DrawOutcome {
        val ordered = candidateIds.distinct().sorted()
        if (ordered.isEmpty()) return DrawOutcome.Empty
        val available = ordered.filterNot { it in drawn }
        if (available.isEmpty()) return DrawOutcome.Exhausted
        val index = if (available.size == 1) 0 else random.nextInt(available.size)
        return DrawOutcome.Selected(DrawSession(ids.next(), available, available[index], index, version, mode, time.now(), filterSummary = summary))
    }
}
/** Storage succeeds before callers can animate. Concurrent prepare/completion is idempotent. */
class DrawCoordinator(private val store: RoundStore, private val engine: DrawEngine, private val time: TimeSource) {
    private val mutex = Mutex()
    private var unsaved: Round? = null
    suspend fun prepare(ids: List<String>, version: String, mode: Presentation, summary: String): DrawOutcome = mutex.withLock {
        val old = store.read()
        old.session?.let { return@withLock DrawOutcome.Selected(it) }
        unsaved?.takeIf { it.version == version }?.let {
            store.save(it)
            unsaved = null
            return@withLock DrawOutcome.Selected(requireNotNull(it.session))
        }
        val round = if (old.version == version) old else Round(version)
        val outcome = engine.prepare(ids, round.drawn, version, mode, summary)
        if (outcome is DrawOutcome.Selected) {
            val prepared = round.copy(session = outcome.session)
            unsaved = prepared
            store.save(prepared)
            unsaved = null
        }
        outcome
    }
    suspend fun reveal(id: String): Round = mutex.withLock { store.reveal(id, time.now()) }
    suspend fun dismiss() = mutex.withLock {
        val r = store.read()
        check(r.session?.status != SessionStatus.Prepared)
        store.save(r.copy(session = null))
    }
    suspend fun restart(version: String) = mutex.withLock {
        check(store.read().session == null)
        store.save(Round(version))
        unsaved = null
    }
}
