package hk.whatoeat

import hk.whatoeat.core.model.*
import hk.whatoeat.domain.MemoryRoundStore
import hk.whatoeat.domain.draw.*
import hk.whatoeat.feature.home.HomeViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private class Prefs(initial: AppPreferences=AppPreferences()) : PreferencesRepository {
        val flow=MutableStateFlow(initial)
        override val preferences=flow
        override suspend fun save(value: AppPreferences) {flow.value=value}
        override suspend fun clear() {flow.value=AppPreferences()}
    }
    private class Saved : FavoritesRepository,HistoryRepository {
        override val favorites=MutableStateFlow(emptyList<SavedFavorite>())
        override val history=MutableStateFlow(emptyList<DrawRecord>())
        override suspend fun toggle(key: String,now: Long) { favorites.value=if(favorites.value.any {it.key==key}) emptyList() else listOf(SavedFavorite(key,now)) }
        override suspend fun delete(drawId: String) {}
        override suspend fun clearHistory() {}
    }
    private val time=TimeSource {1000L}
    private fun restaurant(id: String)=Restaurant("demo",id,id,District.MongKok.point,fetchedAt=1000L)
    private fun vm(dispatcher: CoroutineDispatcher,repo: RestaurantRepository,store: MemoryRoundStore=MemoryRoundStore(),prefs: Prefs=Prefs()): HomeViewModel {
        val saved=Saved();var id=0;val ids=IdSource { "id${++id}" }
        return HomeViewModel(repo,object: LocationRepository {override suspend fun locate()=throw AppFailure(Failure.Permission)},
            saved,saved,prefs,object: AllergenInfoRepository {override suspend fun evidence(key: String)=emptyList<Evidence>()},
            store,DrawCoordinator(store,DrawEngine(RandomSource {0},time,ids),time),time,ids,dispatcher)
    }
    @Test fun oldSearchCannotOverwriteNewSearch() = runTest {
        val d=StandardTestDispatcher(testScheduler);Dispatchers.setMain(d)
        val calls=mutableListOf<CompletableDeferred<List<Restaurant>>>()
        val repo=object: RestaurantRepository {
            override suspend fun nearby(center: Point,radius: Int): List<Restaurant> {
                val response=CompletableDeferred<List<Restaurant>>();calls+=response
                return withContext(NonCancellable) {response.await()}
            }
            override suspend fun details(key: String)=restaurant(key)
        }
        val vm=vm(d,repo)
        try {
            runCurrent();vm.search();runCurrent();vm.selectDistrict(District.Central);runCurrent()
            assertEquals(2,calls.size)
            calls[1].complete(listOf(restaurant("new")));runCurrent()
            calls[0].complete(listOf(restaurant("old")));advanceUntilIdle()
            assertEquals("new",vm.state.value.candidates.single().placeId)
        } finally {vm.viewModelScope.cancel();Dispatchers.resetMain()}
    }
    @Test fun processRestoreRevealsExistingWinnerAndAvoidsAnotherRandomDraw() = runTest {
        val d=StandardTestDispatcher(testScheduler);Dispatchers.setMain(d)
        val store=MemoryRoundStore()
        val prepared=DrawSession("saved",listOf("demo:a","demo:b"),"demo:b",1,"old",Presentation.Wheel,100)
        store.round=Round("old",session=prepared)
        val repo=object: RestaurantRepository {
            override suspend fun nearby(center: Point,radius: Int)=error("must not search")
            override suspend fun details(key: String)=restaurant(key.removePrefix("demo:"))
        }
        val vm=vm(d,repo,store)
        try {
            advanceUntilIdle();assertEquals("demo:b",vm.state.value.session?.winnerId)
            assertEquals(SessionStatus.Revealed,vm.state.value.session?.status)
            assertEquals(setOf("saved"),store.history)
            vm.reveal();vm.reveal();advanceUntilIdle();assertEquals(1,store.history.size)
        } finally {vm.viewModelScope.cancel();Dispatchers.resetMain()}
    }
    @Test fun refusedLocationStillAllowsManualDistrictAndDraw() = runTest {
        val d=StandardTestDispatcher(testScheduler);Dispatchers.setMain(d)
        val repo=object: RestaurantRepository {
            override suspend fun nearby(center: Point,radius: Int)=listOf(restaurant("a").copy(point=center))
            override suspend fun details(key: String)=restaurant("a")
        }
        val vm=vm(d,repo)
        try {
            advanceUntilIdle();vm.permissionDenied(true);vm.selectDistrict(District.ShaTin);vm.search();advanceUntilIdle()
            vm.draw("filters");advanceUntilIdle()
            assertEquals("demo:a",vm.state.value.session?.winnerId)
            assertEquals(SessionStatus.Revealed,vm.state.value.session?.status)
        } finally {vm.viewModelScope.cancel();Dispatchers.resetMain()}
    }
    @Test fun acknowledgmentIsRequiredForUnknownAndChangesWithAvoidancePreferences() = runTest {
        val d=StandardTestDispatcher(testScheduler);Dispatchers.setMain(d)
        val p=Prefs(AppPreferences(filters=Filters(avoid=setOf(Allergen.Milk))))
        val repo=object: RestaurantRepository {
            override suspend fun nearby(center: Point,radius: Int)=listOf(restaurant("a"))
            override suspend fun details(key: String)=restaurant("a")
        }
        val vm=vm(d,repo,prefs=p)
        try {
            advanceUntilIdle();vm.search();advanceUntilIdle();vm.draw("f")
            assertTrue(vm.state.value.needsAcknowledgment);assertNull(vm.state.value.session)
            vm.acknowledgment(true);advanceUntilIdle();assertNotNull(vm.state.value.session)
            vm.dismissResult();advanceUntilIdle()
            vm.applyFilters(Filters(avoid=setOf(Allergen.Eggs)));advanceUntilIdle();vm.draw("f")
            assertTrue(vm.state.value.needsAcknowledgment)
        } finally {vm.viewModelScope.cancel();Dispatchers.resetMain()}
    }
}
