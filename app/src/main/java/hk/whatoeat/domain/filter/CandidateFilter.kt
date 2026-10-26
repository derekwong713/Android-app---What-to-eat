package hk.whatoeat.domain.filter

import hk.whatoeat.core.model.*

enum class Exclusion { Distance, MissingCoordinates, Cuisine, Price, Opening, NotOperating, AllergenConflict, AllergenUnknown }
data class FilterResult(val eligible: List<Restaurant>, val exclusions: Map<String, Set<Exclusion>>,
    val unknownAllergens: Set<String>) {
    val counts: Map<Exclusion, Int> get() = exclusions.values.flatten().groupingBy { it }.eachCount()
}
object CuisineMapping {
    private val mapping = mapOf("chinese_restaurant" to Cuisine.Chinese, "cantonese_restaurant" to Cuisine.Cantonese,
        "japanese_restaurant" to Cuisine.Japanese, "sushi_restaurant" to Cuisine.Japanese,
        "korean_restaurant" to Cuisine.Korean, "thai_restaurant" to Cuisine.Thai,
        "american_restaurant" to Cuisine.Western, "italian_restaurant" to Cuisine.Western,
        "french_restaurant" to Cuisine.Western, "vegetarian_restaurant" to Cuisine.Vegetarian,
        "vegan_restaurant" to Cuisine.Vegetarian, "cafe" to Cuisine.Cafe)
    fun from(types: List<String>): Set<Cuisine> = types.mapNotNull { mapping[it] }.toSet()
    // Places numeric levels: FREE=0 is valid, INEXPENSIVE=1, MODERATE=2, EXPENSIVE=3, VERY_EXPENSIVE=4.
    fun price(level: Int?): Price = when(level) { 0,1 -> Price.Budget; 2 -> Price.Moderate; 3 -> Price.Higher; 4 -> Price.Premium; else -> Price.Unknown }
}
class CandidateFilter(private val evidenceMaxAge: Long = 90L * 24 * 60 * 60 * 1000) {
    fun assess(r: Restaurant, allergen: Allergen, now: Long): EvidenceStatus {
        val applicable = r.evidence.filter { it.allergen == allergen && it.restaurantWide && it.source.isNotBlank() &&
            it.scope.isNotBlank() && it.updatedAt <= now && now - it.updatedAt <= evidenceMaxAge }
        return when {
            applicable.any { it.status == EvidenceStatus.KnownContains } -> EvidenceStatus.KnownContains
            applicable.any { it.status == EvidenceStatus.CrossContactRisk } -> EvidenceStatus.CrossContactRisk
            applicable.any { it.status == EvidenceStatus.SourceReportedAccommodation } -> EvidenceStatus.SourceReportedAccommodation
            else -> EvidenceStatus.Unknown
        }
    }
    fun apply(candidates: List<Restaurant>, center: Point, f: Filters, now: Long): FilterResult {
        val excluded = linkedMapOf<String, Set<Exclusion>>()
        val unknown = mutableSetOf<String>()
        val eligible = candidates.distinctBy { it.key }.sortedBy { it.key }.filter { r ->
            val reasons = mutableSetOf<Exclusion>()
            if (r.point == null || !r.point.usable) reasons += Exclusion.MissingCoordinates
            else if (center.distanceTo(r.point) > f.radius) reasons += Exclusion.Distance
            if (f.cuisines.isNotEmpty() && r.cuisines.intersect(f.cuisines).isEmpty()) reasons += Exclusion.Cuisine
            if (f.prices.isNotEmpty() && r.price !in f.prices && !(r.price == Price.Unknown && f.includeUnknownPrice)) reasons += Exclusion.Price
            if (f.openOnly && r.opening != Opening.Open) reasons += Exclusion.Opening
            if (r.business in setOf(Business.PermanentlyClosed, Business.TemporarilyClosed)) reasons += Exclusion.NotOperating
            val statuses = f.avoid.map { assess(r,it,now) }
            if (statuses.any { it == EvidenceStatus.KnownContains || it == EvidenceStatus.CrossContactRisk }) reasons += Exclusion.AllergenConflict
            if (EvidenceStatus.Unknown in statuses) {
                unknown += r.key
                if (f.allergyMode == AllergyMode.Conservative) reasons += Exclusion.AllergenUnknown
            }
            if (reasons.isNotEmpty()) excluded[r.key] = reasons
            reasons.isEmpty()
        }
        return FilterResult(eligible, excluded, unknown.intersect(eligible.map { it.key }.toSet()))
    }
}
