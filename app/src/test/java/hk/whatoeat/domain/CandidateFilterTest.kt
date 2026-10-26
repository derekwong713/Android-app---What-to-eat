package hk.whatoeat.domain

import hk.whatoeat.core.model.*
import hk.whatoeat.domain.filter.*
import org.junit.Assert.*
import org.junit.Test

class CandidateFilterTest {
    private val now=10000000000L
    private val origin=Point(22.3,114.1)
    private val filter=CandidateFilter()
    private fun r(id: String="a") = Restaurant("test",id,id,origin,setOf(Cuisine.Japanese),Price.Moderate,
        business=Business.Operational,opening=Opening.Open,fetchedAt=now)
    private fun apply(vararg restaurants: Restaurant,f: Filters=Filters()) = filter.apply(restaurants.toList(),origin,f,now)
    @Test fun orWithinAndBetweenCategories() {
        val f=Filters(cuisines=setOf(Cuisine.Japanese,Cuisine.Korean),prices=setOf(Price.Moderate,Price.Higher))
        assertEquals(listOf("a","b"),apply(r(),r("b").copy(cuisines=setOf(Cuisine.Korean),price=Price.Higher),
            r("c").copy(cuisines=setOf(Cuisine.Thai)),r("d").copy(price=Price.Budget),f=f).eligible.map { it.placeId })
    }
    @Test fun missingAndOutOfRangeCoordinatesExcluded() {
        val result=apply(r().copy(point=null),r("b").copy(point=Point(23.0,114.1)))
        assertTrue(result.eligible.isEmpty());assertEquals(1,result.counts[Exclusion.MissingCoordinates]);assertEquals(1,result.counts[Exclusion.Distance])
    }
    @Test fun exactDistanceBoundaryIncluded() {
        val edge=Point(origin.latitude + Math.toDegrees(1000.0/6371000.0),origin.longitude)
        val distance=origin.distanceTo(edge)
        assertEquals(1000.0,distance,1e-6)
        assertEquals(1,apply(r().copy(point=edge),f=Filters(radius=1001)).eligible.size)
        assertEquals(0,apply(r().copy(point=edge),f=Filters(radius=999)).eligible.size)
        val same=filter.apply(listOf(r()),origin,Filters(radius=0),now)
        assertEquals(1,same.eligible.size)
    }
    @Test fun unknownCuisineExcludedOnlyWhenActive() {
        assertEquals(1,apply(r().copy(cuisines=emptySet())).eligible.size)
        assertEquals(0,apply(r().copy(cuisines=emptySet()),f=Filters(cuisines=setOf(Cuisine.Japanese))).eligible.size)
        assertEquals(setOf(Cuisine.Japanese,Cuisine.Korean),CuisineMapping.from(listOf("sushi_restaurant","korean_restaurant","restaurant")))
        assertTrue(CuisineMapping.from(listOf("japanese_name_is_not_a_type")).isEmpty())
    }
    @Test fun everyProviderPriceIncludingFreeIsMapped() {
        assertEquals(listOf(Price.Budget,Price.Budget,Price.Moderate,Price.Higher,Price.Premium),(0..4).map(CuisineMapping::price))
        assertEquals(Price.Unknown,CuisineMapping.price(null));assertEquals(Price.Unknown,CuisineMapping.price(99))
    }
    @Test fun unknownPriceRequiresOptIn() {
        val restaurant=r().copy(price=Price.Unknown)
        assertEquals(1,apply(restaurant).eligible.size)
        val f=Filters(prices=setOf(Price.Budget))
        assertEquals(0,apply(restaurant,f=f).eligible.size)
        assertEquals(1,apply(restaurant,f=f.copy(includeUnknownPrice=true)).eligible.size)
    }
    @Test fun openOnlyExcludesClosedAndUnknown() {
        val list=Opening.entries.mapIndexed { i,o -> r(i.toString()).copy(opening=o) }.toTypedArray()
        assertEquals(3,apply(*list).eligible.size)
        assertEquals(1,apply(*list,f=Filters(openOnly=true)).eligible.size)
    }
    @Test fun nonOperatingAlwaysExcluded() {
        assertEquals(0,apply(r().copy(business=Business.PermanentlyClosed),r("b").copy(business=Business.TemporarilyClosed)).eligible.size)
    }
    private fun evidence(a: Allergen=Allergen.Peanuts,s: EvidenceStatus=EvidenceStatus.KnownContains) = Evidence(a,s,"source",now,"restaurant",true)
    @Test fun onlySelectedAllergensCauseExclusion() {
        val restaurant=r().copy(evidence=listOf(evidence()))
        assertEquals(1,apply(restaurant).eligible.size)
        assertEquals(1,apply(restaurant,f=Filters(avoid=setOf(Allergen.Milk))).eligible.size)
        assertEquals(0,apply(restaurant,f=Filters(avoid=setOf(Allergen.Peanuts))).eligible.size)
    }
    @Test fun crossContactExcludedInBothModes() {
        AllergyMode.entries.forEach { mode -> assertEquals(0,apply(r().copy(evidence=listOf(evidence(s=EvidenceStatus.CrossContactRisk))),f=Filters(avoid=setOf(Allergen.Peanuts),allergyMode=mode)).eligible.size) }
    }
    @Test fun warningKeepsUnknownAndConservativeExcludesIt() {
        val warning=Filters(avoid=setOf(Allergen.Peanuts))
        assertEquals(setOf("test:a"),apply(r(),f=warning).unknownAllergens)
        assertEquals(0,apply(r(),f=warning.copy(allergyMode=AllergyMode.Conservative)).eligible.size)
    }
    @Test fun accommodationMustBeCurrentAndApplicable() {
        val f=Filters(avoid=setOf(Allergen.Peanuts),allergyMode=AllergyMode.Conservative)
        val accommodation=evidence(s=EvidenceStatus.SourceReportedAccommodation)
        assertEquals(1,apply(r().copy(evidence=listOf(accommodation)),f=f).eligible.size)
        listOf(accommodation.copy(restaurantWide=false),accommodation.copy(updatedAt=0),accommodation.copy(updatedAt=now+1),accommodation.copy(source="")).forEach { e ->
            assertEquals(0,apply(r().copy(evidence=listOf(e)),f=f).eligible.size)
        }
    }
    @Test fun conflictWinsOverAccommodation() {
        val e=listOf(evidence(s=EvidenceStatus.SourceReportedAccommodation),evidence())
        assertEquals(0,apply(r().copy(evidence=e),f=Filters(avoid=setOf(Allergen.Peanuts))).eligible.size)
    }
    @Test fun noAutomaticRelaxationAndResetPreservesAllergies() {
        val f=Filters(radius=500,cuisines=setOf(Cuisine.Korean),avoid=setOf(Allergen.Milk),allergyMode=AllergyMode.Conservative)
        assertTrue(apply(r(),f=f).eligible.isEmpty());assertEquals(500,f.radius)
        assertEquals(f.avoid,f.resetGeneral().avoid);assertEquals(f.allergyMode,f.resetGeneral().allergyMode)
        assertNotEquals(f.acknowledgmentKey(),f.copy(avoid=setOf(Allergen.Eggs)).acknowledgmentKey())
    }
}
