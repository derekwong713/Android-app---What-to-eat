package hk.whatoeat.core.model

import kotlin.math.*

data class Point(val latitude: Double, val longitude: Double) {
    val usable get()=latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0
    fun distanceTo(other: Point): Double {
        val a=sin(Math.toRadians(other.latitude-latitude)/2).pow(2)+cos(Math.toRadians(latitude))*cos(Math.toRadians(other.latitude))*sin(Math.toRadians(other.longitude-longitude)/2).pow(2)
        return 6371000.0*2*atan2(sqrt(a.coerceIn(0.0,1.0)),sqrt((1-a).coerceIn(0.0,1.0)))
    }
}
data class LocationFix(val point: Point,val timestamp: Long,val accuracy: Float,val approximate: Boolean)
fun interface TimeSource {fun now(): Long}
fun interface RandomSource {fun nextInt(bound: Int): Int}
fun interface IdSource {fun next(): String}
interface LocationRepository {suspend fun locate(): LocationFix}
enum class Failure {Configuration,Network,Permission,PermissionPermanent,LocationDisabled,LocationUnavailable,LocationTimeout,StaleLocation,Persistence,DetailsUnavailable,Login,Cancelled,Conflict,RateLimited,InvalidInput,SnapshotExpired,Forbidden,Navigation}
class AppFailure(val kind: Failure): Exception(kind.name)

data class Cuisine(val id: String,val label: String)
enum class Business {Unknown,Operating,TemporarilyClosed,Closed}
data class Restaurant(
    val id: String,val name: String,val address: String,val point: Point?,val cuisines: List<Cuisine>,
    val openingHours: String?,val status: Business,val enabled: Boolean=true,val source: String,
    val updatedAt: String,val version: Long,val averageRating: Double?=null,val ratingCount: Int=0
) {val recommendable get()=enabled && status !in setOf(Business.Closed,Business.TemporarilyClosed)}
object DistanceOptions {val metres: List<Int?> = listOf(null,500,1000,2000,5000);const val MAX_FIX_AGE=300000L}
data class Filters(val radius: Int?=null,val cuisines: Set<String> = emptySet()) {
    init {require(radius in DistanceOptions.metres)}
}
data class CuisineCount(val cuisine: Cuisine,val count: Int)
data class CandidateSnapshot(val id: String,val token: String,val expiresAt: Long,val cuisines: List<CuisineCount>,val restaurantCount: Int,val unclassifiedCount: Int)
enum class DrawMode {NewCuisine,SameCuisine}
data class DrawRequest(val id: String,val snapshot: CandidateSnapshot,val mode: DrawMode,val previousCuisine: String?,val previousRestaurant: String?)
data class DrawSession(val drawId: String,val snapshot: CandidateSnapshot,val cuisineId: String,val cuisineIndex: Int,val restaurant: Restaurant,val restaurantCount: Int,val revealed: Boolean=false)
data class SavedDraw(val request: DrawRequest,val session: DrawSession?)
data class Review(val id: String,val nickname: String,val text: String,val createdAt: String,val own: Boolean)
data class Detail(val restaurant: Restaurant,val reviews: List<Review>,val ownRating: Int?,val nextReviewTime: String?=null,val nextReviewId: String?=null)
data class Revision(val id: Long,val version: Long,val nickname: String,val createdAt: String,val before: String,val after: String)
data class Report(val id: String,val restaurantId: String?,val commentId: String?,val reason: String,val createdAt: String)
data class EditPatch(val name: String,val address: String,val point: Point?,val cuisineIds: Set<String>,val openingHours: String?,val status: Business)
data class User(val id: String,val nickname: String,val demo: Boolean=false)
enum class Provider(val wire: String) {Google("google"),Facebook("facebook")}
data class PendingAction(val kind: String,val restaurantId: String)
enum class Presentation {Direct,Wheel}
data class SavedFavorite(val key: String,val createdAt: Long)
data class DrawRecord(val drawId: String,val key: String,val revealedAt: Long,val mode: Presentation,val summary: String)
object Validation {
    fun restaurant(p: EditPatch): Boolean=p.name.trim().length in 1..120 && p.address.length<=500 && (p.openingHours?.length ?: 0)<=1000 && (p.point==null || p.point.usable)
    fun review(text: String)=text.trim().length in 1..2000
    fun nickname(text: String)=text.trim().length in 1..40 && !text.contains('@')
}
