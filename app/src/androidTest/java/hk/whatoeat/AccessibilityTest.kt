package hk.whatoeat
import hk.whatoeat.core.model.*
import hk.whatoeat.data.places.DemoRestaurantRepository
import hk.whatoeat.data.auth.DemoAuthRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
class AccessibilityTest {
 @Test fun demoGuestCannotWriteAndLoginCanUpdateSingleRating()=runBlocking {val auth=DemoAuthRepository();val repo=DemoRestaurantRepository(auth,TimeSource {1000},RandomSource {0});try {repo.rate("demo-1",5);fail()} catch(e: AppFailure) {assertEquals(Failure.Login,e.kind)};auth.begin(Provider.Google,PendingAction("rate","demo-1"));repo.rate("demo-1",5);repo.rate("demo-1",3);val detail=repo.detail("demo-1");assertEquals(1,detail.restaurant.ratingCount);assertEquals(3.0,detail.restaurant.averageRating!!,0.0)}
}
