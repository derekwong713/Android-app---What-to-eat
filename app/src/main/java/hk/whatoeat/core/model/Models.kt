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
    val updatedAt: String,val version: Long,val averageRating: Double?=null,val ratingCount: Int=0,val mapsUrl: String?=null,val region: String="unknown",val locality: String=""
) {val recommendable get()=enabled && status !in setOf(Business.Closed,Business.TemporarilyClosed)}
object DistanceOptions {val metres: List<Int?> = listOf(null,500,1000,2000,5000);const val MAX_FIX_AGE=300000L}
object RatingOptions {val values: List<Double?> = listOf(null,3.0,3.5,4.0,4.5,5.0)}
object RegionOptions {
    val groups=linkedMapOf(
        "hk_island" to listOf("central_western","wan_chai","eastern","southern"),
        "kowloon" to listOf("yau_tsim_mong","sham_shui_po","kowloon_city","wong_tai_sin","kwun_tong"),
        "new_territories" to listOf("kwai_tsing","tsuen_wan","tuen_mun","yuen_long","north","tai_po","sha_tin","sai_kung","islands")
    )
    val groupLabels=mapOf("hk_island" to "香港島","kowloon" to "九龍","new_territories" to "新界")
    val labels=linkedMapOf(
        "central_western" to "中西區",
        "wan_chai" to "灣仔區",
        "eastern" to "東區",
        "southern" to "南區",
        "yau_tsim_mong" to "油尖旺區",
        "sham_shui_po" to "深水埗區",
        "kowloon_city" to "九龍城區",
        "wong_tai_sin" to "黃大仙區",
        "kwun_tong" to "觀塘區",
        "kwai_tsing" to "葵青區",
        "tsuen_wan" to "荃灣區",
        "tuen_mun" to "屯門區",
        "yuen_long" to "元朗區",
        "north" to "北區",
        "tai_po" to "大埔區",
        "sha_tin" to "沙田區",
        "sai_kung" to "西貢區",
        "islands" to "離島區",
        "unknown" to "地區未知"
    )
    val allLabels get()=labels+LocalityOptions.labels
    fun matches(selected: Set<String>,r: Restaurant)=selected.isEmpty() || r.region in selected || (r.locality.isNotBlank() && r.locality in selected)
    fun migrate(values: Set<String>): Set<String> = values.flatMap {groups[it] ?: listOf(it)}.filter {it in allLabels}.toSet()
}

data class Filters(val radius: Int?=null,val cuisines: Set<String> = emptySet(),val minRating: Double?=null,val regions: Set<String> = emptySet()) {
    val effectiveRadius get()=radius.takeIf {regions.isEmpty()}
    fun normalized()=if(regions.isNotEmpty() && radius!=null) copy(radius=null) else this
    init {require(radius in DistanceOptions.metres);require(minRating in RatingOptions.values);require(regions.all {it in RegionOptions.allLabels})}
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
