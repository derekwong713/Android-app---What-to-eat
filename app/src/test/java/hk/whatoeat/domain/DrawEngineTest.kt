package hk.whatoeat.domain
import hk.whatoeat.core.model.*
import hk.whatoeat.domain.draw.DrawEngine
import org.junit.Assert.*
import org.junit.Test
class DrawEngineTest {
 private val snap=CandidateSnapshot("snapshot","token",1000,listOf(CuisineCount(a,2),CuisineCount(b,1)),2,0)
 private fun req(mode: DrawMode=DrawMode.NewCuisine,c: String?=null,r: String?=null)=DrawRequest("id",snap,mode,c,r)
 @Test fun cuisineThenRestaurantUsesDistinctUniformBounds() {val bounds=mutableListOf<Int>();val engine=DrawEngine {n -> bounds+=n;0};val result=engine.prepare(req(),listOf(row("1",listOf(a,b)),row("2"),row("2")))!!;assertEquals(listOf(2,2),bounds);assertEquals("a",result.cuisineId);assertEquals(2,result.restaurantCount)}
 @Test fun rerollAvoidsPreviousAtEachStage() {val engine=DrawEngine {0};val rows=listOf(row("1",listOf(a,b)),row("2",listOf(a,b)));val pick=engine.prepare(req(c="a",r="1"),rows)!!;assertEquals("b",pick.cuisineId);assertEquals("2",pick.restaurant.id)}
 @Test fun restaurantRerollKeepsCuisine() {val pick=DrawEngine {0}.prepare(req(DrawMode.SameCuisine,"a","1"),listOf(row("1"),row("2")))!!;assertEquals("a",pick.cuisineId);assertEquals("2",pick.restaurant.id)}
 @Test fun singletonIsValidWithoutRandomCall() {val single=snap.copy(cuisines=listOf(CuisineCount(a,1)));val pick=DrawEngine {error("No randomness needed")}.prepare(req().copy(snapshot=single,previousCuisine="a",previousRestaurant="1"),listOf(row()))!!;assertEquals(1,pick.restaurantCount)}
 @Test fun zeroAndMissingCuisineProduceNoResult() {val engine=DrawEngine {error("No randomness")};assertNull(engine.prepare(req().copy(snapshot=snap.copy(cuisines=emptyList())),emptyList()));assertNull(engine.prepare(req(DrawMode.SameCuisine,"missing"),listOf(row())))}
}
