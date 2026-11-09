package hk.whatoeat.domain
import hk.whatoeat.core.model.*
import hk.whatoeat.domain.filter.CandidateFilter
import org.junit.Assert.*
import org.junit.Test
val a=Cuisine("a","A");val b=Cuisine("b","B")
fun row(id: String="1",cs: List<Cuisine> = listOf(a),point: Point?=Point(22.3,114.1),status: Business=Business.Unknown)=Restaurant(id,"店名","地址",point,cs,null,status,source="test",updatedAt="now",version=1)
class CandidateFilterTest {
 private val filter=CandidateFilter();private val fix=LocationFix(Point(22.3,114.1),100,10f,false)
 @Test fun distanceCuisineAndStatusAlwaysApply() {val result=filter.apply(listOf(row(),row("2",listOf(b)),row("3",point=Point(23.0,114.1)),row("4",status=Business.Closed)),Filters(500,setOf("a")),fix,100);assertEquals(listOf("1"),result.eligible.map {it.id})}
 @Test fun ratingAndCuisineStillApplyWhileRegionOverridesDistance() {
 val good=row().copy(averageRating=4.0,region="yau_tsim_mong")
 val rows=listOf(good,good.copy(id="low",averageRating=3.9),good.copy(id="missing",averageRating=null),good.copy(id="island",region="central_western"),good.copy(id="far",point=Point(22.5,114.1)),good.copy(id="other",cuisines=listOf(b)))
 val filters=Filters(500,setOf("a"),4.0,setOf("yau_tsim_mong"))
 assertEquals(listOf("1","far"),filter.apply(rows,filters,null,100).eligible.map {it.id})
 }
 @Test fun missingRatingOrRegionCanStillPassWhenUnrestricted() {
 val unknown=row()
 assertEquals(1,filter.apply(listOf(unknown),Filters(),null,100).eligible.size)
 assertTrue(filter.apply(listOf(unknown),Filters(minRating=3.0),null,100).eligible.isEmpty())
 assertTrue(filter.apply(listOf(unknown),Filters(regions=setOf("yau_tsim_mong")),null,100).eligible.isEmpty())
 }
 @Test fun rerollsOnlyUseRatingAndRegionFilteredCandidates() {
 val filters=Filters(minRating=4.0,regions=setOf("yau_tsim_mong"))
 val rows=listOf(row("1").copy(averageRating=4.1,region="yau_tsim_mong"),row("2").copy(averageRating=4.5,region="yau_tsim_mong"),row("3").copy(averageRating=3.0,region="yau_tsim_mong"),row("4").copy(averageRating=5.0,region="central_western"))
 val eligible=filter.apply(rows,filters,null,100).eligible
 val snapshot=CandidateSnapshot("s","token",1000,filter.cuisines(eligible,filters),eligible.size,0)
 val engine=hk.whatoeat.domain.draw.DrawEngine(RandomSource {0})
 val first=engine.prepare(DrawRequest("first",snapshot,DrawMode.NewCuisine,null,null),eligible)!!
 val next=engine.prepare(DrawRequest("next",snapshot,DrawMode.SameCuisine,first.cuisineId,first.restaurant.id),eligible)!!
 assertEquals(setOf("1","2"),setOf(first.restaurant.id,next.restaurant.id))
 assertEquals(2,next.restaurantCount)
 }
 @Test fun filtersNeverRelaxWhenRatingAndRegionHaveNoMatch() {
 val filters=Filters(minRating=5.0,regions=setOf("yau_tsim_mong","central_western"))
 assertTrue(filter.apply(listOf(row().copy(averageRating=4.9,region="yau_tsim_mong")),filters,null,100).eligible.isEmpty())
 }
 @Test fun districtSelectionCanDistinguishTwoDistrictsOnSameSide() {
 val rows=listOf(row("1").copy(region="yau_tsim_mong"),row("2").copy(region="kwun_tong"),row("3").copy(region="islands"))
 assertEquals(listOf("2"),filter.apply(rows,Filters(regions=setOf("kwun_tong")),null,100).eligible.map {it.id})
 assertEquals(listOf("1","2"),filter.apply(rows,Filters(regions=setOf("kwun_tong","yau_tsim_mong")),null,100).eligible.map {it.id})
 }
 @Test fun localitySelectionIsMorePreciseThanDistrictAndSupportsUnion() {
 val rows=listOf(row("1").copy(region="yau_tsim_mong",locality="area_mong_kok"),row("2").copy(region="yau_tsim_mong",locality="area_jordan"),row("3").copy(region="islands",locality="area_tung_chung"))
 assertEquals(listOf("1"),filter.apply(rows,Filters(regions=setOf("area_mong_kok")),null,100).eligible.map {it.id})
 assertEquals(listOf("1","3"),filter.apply(rows,Filters(regions=setOf("area_mong_kok","islands")),null,100).eligible.map {it.id})
 }
 @Test fun selectedAreaForcesUnlimitedDistanceEvenWithoutPermission() {
 val filters=Filters(radius=500,regions=setOf("area_mong_kok"))
 assertNull(filters.effectiveRadius)
 assertNull(filters.normalized().radius)
 val far=row().copy(point=Point(22.5,114.1),locality="area_mong_kok",region="yau_tsim_mong")
 assertEquals(1,filter.apply(listOf(far),filters,null,100).eligible.size)
 val cleared=filters.normalized().copy(regions=emptySet())
 assertNull(cleared.radius)
 assertEquals(500,cleared.copy(radius=500).effectiveRadius)
 }
 @Test fun restoredPlacesHaveDistrictMembershipAndMixedCoverageIsExplicit() {
 assertEquals(setOf("yau_tsim_mong"),LocalityOptions.memberships["area_prince_edward"])
 assertEquals(setOf("eastern","wan_chai"),LocalityOptions.memberships["area_causeway_bay"])
 assertFalse(LocalityOptions.covered("area_causeway_bay",setOf("wan_chai")))
 assertTrue(LocalityOptions.covered("area_causeway_bay",setOf("wan_chai","eastern")))
 }
 @Test fun oldBroadRegionPreferencesExpandIntoDistricts() {
 assertEquals(5,RegionOptions.migrate(setOf("kowloon")).size)
 assertTrue("yau_tsim_mong" in RegionOptions.migrate(setOf("kowloon")))
 assertEquals(setOf("sha_tin"),RegionOptions.migrate(setOf("unknown","sha_tin","invalid")))
 assertTrue(RegionOptions.migrate(setOf("unknown")).isEmpty())
 assertEquals(18,RegionOptions.migrate(setOf("hk_island","kowloon","new_territories")).size)
 }
 @Test fun unlimitedDoesNotNeedFakeLocation() {assertEquals(1,filter.apply(listOf(row(point=null)),Filters(),null,100).eligible.size)}
 @Test fun unclassifiedAndDuplicatesAreExplicit() {val r=row(cs=listOf(a,b,a));val result=filter.apply(listOf(r,r,row("2",emptyList())),Filters(),null,100);assertEquals(1,result.eligible.size);assertEquals(1,result.unclassified);assertEquals(listOf(1,1),filter.cuisines(result.eligible,Filters()).map {it.count})}
 @Test fun missingHoursAndStatusAreNotClosed() {assertTrue(row().recommendable);assertFalse(row(status=Business.TemporarilyClosed).recommendable)}
 @Test fun zeroDoesNotRelaxDistance() {assertTrue(filter.apply(listOf(row(point=Point(0.0,0.0))),Filters(500),fix,100).eligible.isEmpty())}
 @Test fun deniedAndStaleLocationFailFiniteDistance() {for(f in listOf(null,fix.copy(timestamp=-400000))) {try {filter.apply(listOf(row()),Filters(500),f,100);fail()} catch(e: AppFailure) {assertEquals(Failure.LocationUnavailable,e.kind)}}}
}
