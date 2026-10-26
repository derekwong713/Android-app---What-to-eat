package hk.whatoeat.core.model

import kotlin.math.*

data class Point(val latitude: Double, val longitude: Double) {
    val usable: Boolean get() = latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0
    fun distanceTo(other: Point): Double {
        val dLat = Math.toRadians(other.latitude - latitude)
        val dLon = Math.toRadians(other.longitude - longitude)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(latitude)) * cos(Math.toRadians(other.latitude)) * sin(dLon / 2).pow(2)
        return 6371000.0 * 2 * atan2(sqrt(a.coerceIn(0.0, 1.0)), sqrt((1 - a).coerceIn(0.0, 1.0)))
    }
}
enum class Cuisine { Cantonese, Chinese, Japanese, Korean, Western, Thai, Vegetarian, Cafe }
enum class Price { Budget, Moderate, Higher, Premium, Unknown }
enum class Business { Operational, TemporarilyClosed, PermanentlyClosed, Unknown }
enum class Opening { Open, Closed, Unknown }
enum class Allergen { Peanuts, TreeNuts, Milk, Eggs, Fish, Crustaceans, Molluscs, Soy, Wheat, Sesame }
enum class EvidenceStatus { KnownContains, CrossContactRisk, Unknown, SourceReportedAccommodation }
enum class AllergyMode { Warning, Conservative }
enum class Presentation { Direct, Wheel }
enum class SessionStatus { Prepared, Revealed }
data class Evidence(val allergen: Allergen, val status: EvidenceStatus, val source: String,
    val updatedAt: Long, val scope: String, val restaurantWide: Boolean)
data class Restaurant(val provider: String, val placeId: String, val name: String?, val point: Point?,
    val cuisines: Set<Cuisine> = emptySet(), val price: Price = Price.Unknown,
    val rating: Double? = null, val ratingCount: Int? = null, val business: Business = Business.Unknown,
    val opening: Opening = Opening.Unknown, val address: String? = null, val phone: String? = null,
    val website: String? = null, val fetchedAt: Long, val attributions: List<String> = emptyList(),
    val evidence: List<Evidence> = emptyList(), val openingText: List<String> = emptyList()) {
    val key: String get() = "$provider:$placeId"
}
data class Filters(val radius: Int = 1000, val cuisines: Set<Cuisine> = emptySet(), val prices: Set<Price> = emptySet(),
    val includeUnknownPrice: Boolean = false, val openOnly: Boolean = false,
    val avoid: Set<Allergen> = emptySet(), val allergyMode: AllergyMode = AllergyMode.Warning) {
    fun acknowledgmentKey() = "allergy-policy-1|${avoid.sortedBy { it.name }.joinToString { it.name }}|${allergyMode.name}"
    fun resetGeneral() = Filters(avoid = avoid, allergyMode = allergyMode)
}
data class AppPreferences(val filters: Filters = Filters(), val reducedMotion: Boolean = false,
    val mode: Presentation = Presentation.Direct, val acknowledged: String? = null)
data class LocationFix(val point: Point, val timestamp: Long, val accuracy: Float, val approximate: Boolean)
enum class District(val point: Point) {
    MongKok(Point(22.3193,114.1694)), CausewayBay(Point(22.2800,114.1840)), Central(Point(22.2819,114.1589)),
    ShaTin(Point(22.3814,114.1889)), TsuenWan(Point(22.3710,114.1140))
}
enum class Failure { Network, Permission, Configuration, QuotaService, LocationDisabled, LocationUnavailable,
    LocationTimeout, PermissionPermanent, Persistence, DetailsUnavailable, Navigation, StaleLocation }
class AppFailure(val kind: Failure) : Exception(kind.name)
data class SavedFavorite(val key: String, val createdAt: Long)
data class DrawRecord(val drawId: String, val key: String, val revealedAt: Long, val mode: Presentation, val summary: String)
data class DrawSession(val drawId: String, val orderedCandidateIds: List<String>, val winnerId: String,
    val winnerIndex: Int, val candidateVersion: String, val presentationMode: Presentation, val createdAt: Long,
    val status: SessionStatus = SessionStatus.Prepared, val filterSummary: String = "")
data class Round(val version: String = "", val drawn: Set<String> = emptySet(), val session: DrawSession? = null)
