package hk.whatoeat
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import hk.whatoeat.core.model.*
import hk.whatoeat.data.local.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
class LocalStoreTest {
 @get:Rule val helper=MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),EatDatabase::class.java.canonicalName!!,FrameworkSQLiteOpenHelperFactory())
 @Test fun legacyMigrationPreservesRows() {helper.createDatabase("migration.db",1).apply {execSQL("INSERT INTO favorites VALUES ('google:old',100)");execSQL("INSERT INTO history VALUES ('old','google:old',100,'Direct','legacy')");close()};helper.runMigrationsAndValidate("migration.db",2,true,EatDatabase.MIGRATION_1_2).apply {query("SELECT COUNT(*) FROM favorites").use {it.moveToFirst();assertEquals(1,it.getInt(0))};query("SELECT COUNT(*) FROM history").use {it.moveToFirst();assertEquals(1,it.getInt(0))};close()}}
 @Test fun atomicRevealIsUniqueAndHistoryBounded()=runBlocking {
 val db=Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,EatDatabase::class.java).build()
 try {val store=LocalStore(db);val snap=CandidateSnapshot("s","t",999999,listOf(CuisineCount(Cuisine("a","中式"),1)),1,0)
 for(i in 0..204) {val request=DrawRequest("id$i",snap,DrawMode.NewCuisine,null,null);val session=DrawSession("id$i",snap,"a",0,testRestaurant(),1,true);store.save(SavedDraw(request,session));store.save(SavedDraw(request,session))}
 assertEquals(200,store.history.first().size);assertEquals(200,store.history.first().map {it.drawId}.distinct().size);assertTrue(store.read()!!.session!!.revealed)
 } finally {db.close()}}
}
