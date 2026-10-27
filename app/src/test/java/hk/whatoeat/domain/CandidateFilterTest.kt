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
 @Test fun unlimitedDoesNotNeedFakeLocation() {assertEquals(1,filter.apply(listOf(row(point=null)),Filters(),null,100).eligible.size)}
 @Test fun unclassifiedAndDuplicatesAreExplicit() {val r=row(cs=listOf(a,b,a));val result=filter.apply(listOf(r,r,row("2",emptyList())),Filters(),null,100);assertEquals(1,result.eligible.size);assertEquals(1,result.unclassified);assertEquals(listOf(1,1),filter.cuisines(result.eligible,Filters()).map {it.count})}
 @Test fun missingHoursAndStatusAreNotClosed() {assertTrue(row().recommendable);assertFalse(row(status=Business.TemporarilyClosed).recommendable)}
 @Test fun zeroDoesNotRelaxDistance() {assertTrue(filter.apply(listOf(row(point=Point(0.0,0.0))),Filters(500),fix,100).eligible.isEmpty())}
 @Test fun deniedAndStaleLocationFailFiniteDistance() {for(f in listOf(null,fix.copy(timestamp=-400000))) {try {filter.apply(listOf(row()),Filters(500),f,100);fail()} catch(e: AppFailure) {assertEquals(Failure.LocationUnavailable,e.kind)}}}
}
