package hk.whatoeat.data.places

// Provider migration keeps the existing repository file/entry point, now backed solely by Supabase.
// There is deliberately no Google Maps, Places, Geocoding or Routes SDK/API call.
import hk.whatoeat.core.model.*
import hk.whatoeat.data.backend.*
import hk.whatoeat.data.backend.Codec.objects
import org.json.*

class SupabaseRestaurantRepository(private val http: JsonTransport,private val auth: AuthRepository): RestaurantRepository {
    private suspend fun rpc(name: String,args: JSONObject=JSONObject(),write: Boolean=false): Any {
        val access=try {auth.token()} catch(e: AppFailure) {if(!write && e.kind==Failure.Login) null else throw e}
        if(write && access==null) throw AppFailure(Failure.Login)
        return http.request("POST","/rest/v1/rpc/eat_$name",args,access)
    }
    override suspend fun cuisines()=(rpc("list_cuisines") as JSONArray).objects().map(Codec::cuisine)
    override suspend fun snapshot(filters: Filters,location: LocationFix?): CandidateSnapshot {
        if(filters.radius!=null && location==null) throw AppFailure(Failure.LocationUnavailable)
        return Codec.snapshot(rpc("create_snapshot",JSONObject().put("p_latitude",if(filters.radius!=null) location?.point?.latitude else JSONObject.NULL).put("p_longitude",if(filters.radius!=null) location?.point?.longitude else JSONObject.NULL).put("p_radius",filters.radius ?: JSONObject.NULL).put("p_cuisines",JSONArray(filters.cuisines.sorted()))) as JSONObject)
    }
    override suspend fun prepare(request: DrawRequest): DrawSession = Codec.session(rpc("prepare_draw",JSONObject().put("p_snapshot_id",request.snapshot.id).put("p_token",request.snapshot.token).put("p_draw_id",request.id).put("p_mode",if(request.mode==DrawMode.NewCuisine) "new_cuisine" else "same_cuisine").put("p_previous_cuisine",request.previousCuisine ?: JSONObject.NULL).put("p_previous_restaurant",request.previousRestaurant ?: JSONObject.NULL)) as JSONObject,request.snapshot)
    override suspend fun detail(id: String,before: String?,beforeId: String?): Detail {
        val j=rpc("restaurant_detail",JSONObject().put("p_id",id).put("p_before",before ?: JSONObject.NULL).put("p_before_id",beforeId ?: JSONObject.NULL)) as JSONObject
        val reviews=j.getJSONArray("reviews").objects().map {Review(it.getString("id"),it.getString("nickname"),it.getString("body"),it.getString("created_at"),it.optBoolean("own"))}
        val last=reviews.lastOrNull().takeIf {reviews.size==10}
        return Detail(Codec.restaurant(j.getJSONObject("restaurant")),reviews,if(j.isNull("own_rating")) null else j.getInt("own_rating"),last?.createdAt,last?.id)
    }
    override suspend fun edit(id: String,version: Long,patch: EditPatch) {rpc("edit_restaurant",JSONObject().put("p_id",id).put("p_expected_version",version).put("p_patch",Codec.patch(patch)),true)}
    override suspend fun rate(id: String,stars: Int) {rpc("rate",JSONObject().put("p_id",id).put("p_stars",stars),true)}
    override suspend fun comment(id: String,text: String,requestId: String) {rpc("comment",JSONObject().put("p_id",id).put("p_body",text).put("p_request_id",requestId),true)}
    override suspend fun deleteComment(id: String) {rpc("delete_comment",JSONObject().put("p_comment_id",id),true)}
    override suspend fun revisions(id: String,beforeVersion: Long?)=(rpc("revisions",JSONObject().put("p_id",id).put("p_before_version",beforeVersion ?: JSONObject.NULL)) as JSONArray).objects().map {Revision(it.getLong("id"),it.getLong("version"),it.getString("nickname"),it.getString("created_at"),it.getJSONObject("before").toString(2),it.getJSONObject("after").toString(2))}
    override suspend fun restore(id: String,revisionId: Long,version: Long) {rpc("restore_revision",JSONObject().put("p_id",id).put("p_revision_id",revisionId).put("p_expected_version",version),true)}
    override suspend fun report(restaurantId: String?,commentId: String?,reason: String) {rpc("report",JSONObject().put("p_restaurant_id",restaurantId ?: JSONObject.NULL).put("p_comment_id",commentId ?: JSONObject.NULL).put("p_reason",reason),true)}
    override suspend fun admin()=(rpc("is_admin") as? Boolean)==true
    override suspend fun reports(before: String?)=(rpc("list_reports",JSONObject().put("p_before",before ?: JSONObject.NULL),true) as JSONArray).objects().map {Report(it.getString("id"),it.optString("restaurant_id").takeUnless {v -> v=="null" || v.isBlank()},it.optString("comment_id").takeUnless {v -> v=="null" || v.isBlank()},it.getString("reason"),it.getString("created_at"))}
    override suspend fun resolve(reportId: String,action: String) {rpc("resolve_report",JSONObject().put("p_id",reportId).put("p_action",action),true)}
    override suspend fun nickname(value: String) {rpc("set_nickname",JSONObject().put("p_nickname",value),true)}
    override suspend fun profile()=(rpc("my_profile") as? String)?.takeIf {it.isNotBlank() && it!="null"}
}
