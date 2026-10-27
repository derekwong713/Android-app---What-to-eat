package hk.whatoeat
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import hk.whatoeat.core.model.*
import hk.whatoeat.data.auth.DemoAuthRepository
import hk.whatoeat.data.local.*
import hk.whatoeat.data.places.DemoRestaurantRepository
import hk.whatoeat.feature.home.HomeViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID
class RuntimeFlowTest {
 private suspend fun waitFor(condition: ()->Boolean) {withTimeout(15000) {while(!condition()) delay(20)}}
 @Test fun rapidClicksPreparedPersistenceLoginResumeAndReroll()=runBlocking {
 val context=InstrumentationRegistry.getInstrumentation().targetContext;val db=Room.inMemoryDatabaseBuilder(context,EatDatabase::class.java).build();val store=LocalStore(db);val prefs=UserPreferences(context);val original=prefs.read();prefs.save(Filters())
 val auth=DemoAuthRepository();val base=DemoRestaurantRepository(auth,TimeSource {1000},RandomSource {0});var calls=0
 val repo=object: RestaurantRepository by base {override suspend fun prepare(request: DrawRequest): DrawSession {calls++;delay(80);return base.prepare(request)}}
 val models=ViewModelStore();lateinit var vm: HomeViewModel
 try {
 withContext(Dispatchers.Main) {vm=HomeViewModel(repo,object: LocationRepository {override suspend fun locate(): LocationFix=throw AppFailure(Failure.LocationUnavailable)},auth,store,prefs,TimeSource {1000},IdSource {UUID.randomUUID().toString()},Dispatchers.IO);models.put("test",vm)}
 waitFor {vm.state.value.ready}
 withContext(Dispatchers.Main) {repeat(30) {vm.draw()}}
 waitFor {!vm.state.value.busy && vm.state.value.session!=null}
 val first=vm.state.value.session!!;assertEquals(1,calls);assertEquals(first,store.read()!!.session)
 withContext(Dispatchers.Main) {repeat(10) {vm.reveal()}}
 waitFor {vm.state.value.session?.revealed==true && !vm.state.value.detailLoading};assertEquals(1,store.history.first().size)
 withContext(Dispatchers.Main) {vm.draw(DrawMode.SameCuisine)}
 waitFor {!vm.state.value.busy && vm.state.value.session?.drawId!=first.drawId}
 val second=vm.state.value.session!!;assertEquals(first.cuisineId,second.cuisineId);assertNotEquals(first.restaurant.id,second.restaurant.id);assertEquals(2,calls)
 withContext(Dispatchers.Main) {vm.requestAction("rate",second.restaurant.id)};assertTrue(vm.state.value.login)
 withContext(Dispatchers.Main) {vm.signIn(Provider.Google)};waitFor {!vm.state.value.busy && !vm.state.value.login};assertEquals("rate",vm.state.value.action?.kind);assertEquals(second.restaurant.id,vm.state.value.detail?.restaurant?.id)
 withContext(Dispatchers.Main) {vm.rate(second.restaurant.id,4)};waitFor {!vm.state.value.busy};assertEquals(4.0,vm.state.value.detail!!.restaurant.averageRating!!,0.0)
 withContext(Dispatchers.Main) {vm.closeAction();vm.applyFilters(Filters(500))};waitFor {!vm.state.value.busy}
 withContext(Dispatchers.Main) {vm.draw()};waitFor {!vm.state.value.busy};assertEquals(Failure.LocationUnavailable,vm.state.value.failure);assertEquals(2,calls)
 withContext(Dispatchers.Main) {vm.clearFailure();vm.noDistance()};waitFor {!vm.state.value.busy};assertNull(vm.state.value.filters.radius)
 } finally {withContext(Dispatchers.Main) {models.clear()};prefs.save(original);db.close()}
 }
 @Test fun finiteCachedSnapshotCannotUseExpiredOrFailedNewLocation()=runBlocking {
 val context=InstrumentationRegistry.getInstrumentation().targetContext;val db=Room.inMemoryDatabaseBuilder(context,EatDatabase::class.java).build();val store=LocalStore(db);val prefs=UserPreferences(context);val original=prefs.read();prefs.save(Filters(500))
 var now=1000L;var failLocation=false;val time=TimeSource {now};val auth=DemoAuthRepository();val repo=DemoRestaurantRepository(auth,time,RandomSource {0});val models=ViewModelStore();lateinit var vm: HomeViewModel
 try {
 withContext(Dispatchers.Main) {vm=HomeViewModel(repo,object: LocationRepository {override suspend fun locate(): LocationFix {if(failLocation) throw AppFailure(Failure.LocationUnavailable);return LocationFix(Point(22.3193,114.1694),now,10f,false)}},auth,store,prefs,time,IdSource {UUID.randomUUID().toString()},Dispatchers.IO);models.put("fresh",vm)}
 waitFor {vm.state.value.ready}
 withContext(Dispatchers.Main) {vm.useLocation()};waitFor {!vm.state.value.busy};assertNotNull(vm.state.value.fix)
 withContext(Dispatchers.Main) {vm.refresh()};waitFor {!vm.state.value.busy};assertNotNull(vm.state.value.snapshot)
 failLocation=true;withContext(Dispatchers.Main) {vm.useLocation()};waitFor {!vm.state.value.busy};assertNull(vm.state.value.fix);assertNull(vm.state.value.snapshot)
 failLocation=false;withContext(Dispatchers.Main) {vm.useLocation()};waitFor {!vm.state.value.busy};withContext(Dispatchers.Main) {vm.refresh()};waitFor {!vm.state.value.busy};assertNotNull(vm.state.value.snapshot)
 now+=DistanceOptions.MAX_FIX_AGE+1
 withContext(Dispatchers.Main) {vm.draw()};waitFor {!vm.state.value.busy};assertEquals(Failure.LocationUnavailable,vm.state.value.failure);assertNull(vm.state.value.session)
 } finally {withContext(Dispatchers.Main) {models.clear()};prefs.save(original);db.close()}
 }

}
