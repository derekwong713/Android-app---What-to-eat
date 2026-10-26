package hk.whatoeat

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import hk.whatoeat.core.model.*
import hk.whatoeat.data.local.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LocalStoreTest {
    @Test fun atomicRevealIsUniqueAndHistoryIsBounded() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val db=Room.inMemoryDatabaseBuilder(context,EatDatabase::class.java).build()
        try {
            val store=LocalStore(db)
            for(i in 0..204) {
                val session=DrawSession("draw$i",listOf("google:place$i"),"google:place$i",0,"v",Presentation.Direct,i.toLong())
                store.save(Round("v",session=session));store.reveal(session.drawId,i.toLong());store.reveal(session.drawId,i.toLong())
            }
            assertEquals(200,store.history.first().size)
            assertEquals("draw204",store.history.first().first().drawId)
            assertEquals(SessionStatus.Revealed,store.read().session?.status)
            assertEquals(setOf("google:place204"),store.read().drawn)
        } finally {db.close()}
    }
    @Test fun favoritesAndPendingSessionSurviveDatabaseReopen() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val name="test-restore.db";context.deleteDatabase(name)
        var db=Room.databaseBuilder(context,EatDatabase::class.java,name).build()
        val session=DrawSession("id",listOf("google:place"),"google:place",0,"v",Presentation.Wheel,100)
        try {
            LocalStore(db).apply {toggle("google:place",50);save(Round("v",session=session))};db.close()
            db=Room.databaseBuilder(context,EatDatabase::class.java,name).build()
            val store=LocalStore(db)
            assertEquals("google:place",store.favorites.first().single().key)
            assertEquals(session,store.read().session)
        } finally {db.close();context.deleteDatabase(name)}
    }
}
