package hk.whatoeat

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.*
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import hk.whatoeat.core.model.*
import hk.whatoeat.data.auth.DemoAuthRepository
import hk.whatoeat.data.local.*
import hk.whatoeat.data.places.DemoRestaurantRepository
import hk.whatoeat.feature.home.HomeViewModel
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class LocationCancellationTest {
 @get:Rule val ui=createComposeRule()
 @Test fun cancelledLookupCannotRestoreFixOrBusyAfterLateCompletion() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val db=Room.inMemoryDatabaseBuilder(context,EatDatabase::class.java).build()
  val owner=object:ViewModelStoreOwner {override val viewModelStore=ViewModelStore()}
  val result=CompletableDeferred<LocationFix>();val secondResult=CompletableDeferred<LocationFix>()
  val started=CompletableDeferred<Unit>();val secondStarted=CompletableDeferred<Unit>();val returned=CompletableDeferred<Unit>()
  val calls=java.util.concurrent.atomic.AtomicInteger(0)
  val clock=TimeSource {1000L};val auth=DemoAuthRepository()
  val locations=object:LocationRepository {override suspend fun locate(): LocationFix=withContext(NonCancellable) {
   if(calls.incrementAndGet()==1) {started.complete(Unit);val fix=result.await();returned.complete(Unit);fix}
   else {secondStarted.complete(Unit);secondResult.await()}
  }}
  lateinit var vm: HomeViewModel
  ui.setContent {androidx.compose.material3.Text("定位取消測試")}
  try {
   ui.runOnIdle {
    val factory=object:ViewModelProvider.Factory {
     @Suppress("UNCHECKED_CAST") override fun <T:ViewModel> create(modelClass: Class<T>): T = HomeViewModel(
      DemoRestaurantRepository(auth,clock,RandomSource {0}),locations,auth,LocalStore(db),UserPreferences(context),clock,IdSource {"location-test"},Dispatchers.IO) as T
    }
    vm=ViewModelProvider(owner,factory)[HomeViewModel::class.java]
   }
   ui.waitUntil(10000) {vm.state.value.ready}
   ui.runOnIdle {vm.useLocation()}
   ui.waitUntil(10000) {started.isCompleted}
   ui.runOnIdle {vm.cancelLocation()}
   assertFalse(vm.state.value.busy);assertNull(vm.state.value.fix)
   ui.runOnIdle {vm.useLocation()}
   ui.waitUntil(10000) {secondStarted.isCompleted}
   result.complete(LocationFix(Point(22.3,114.1),1000,10f,false))
   ui.waitUntil(10000) {returned.isCompleted}
   ui.runOnIdle {assertNull(vm.state.value.fix);assertTrue(vm.state.value.busy);assertNull(vm.state.value.failure)}
   secondResult.complete(LocationFix(Point(22.4,114.2),1000,10f,false))
   ui.waitUntil(10000) {vm.state.value.fix?.point==Point(22.4,114.2) && !vm.state.value.busy}
  } finally {
   result.complete(LocationFix(Point(22.3,114.1),1000,10f,false))
   secondResult.complete(LocationFix(Point(22.4,114.2),1000,10f,false))
   ui.runOnIdle {owner.viewModelStore.clear()};db.close()
  }
 }
}
