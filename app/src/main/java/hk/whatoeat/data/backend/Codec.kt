package hk.whatoeat.data.backend
import hk.whatoeat.core.model.*
import org.json.*
object Codec {
    fun JSONArray.objects()=(0 until length()).map {getJSONObject(it)}
    fun JSONArray.strings()=(0 until length()).map {getString(it)}
    fun cuisine(j: JSONObject)=Cuisine(j.getString("id"),j.getString("label"))
    fun cuisine(c: Cuisine)=JSONObject().put("id",c.id).put("label",c.label)
    fun restaurant(j: JSONObject): Restaurant {
        val p=if(j.isNull("latitude") || j.isNull("longitude")) null else Point(j.getDouble("latitude"),j.getDouble("longitude"))
        return Restaurant(j.getString("id"),j.getString("name"),j.optString("address"),p,j.getJSONArray("cuisines").objects().map(::cuisine),j.optString("opening_hours").takeIf {it.isNotBlank() && it!="null"},when(j.optString("status")) {"operating"->Business.Operating;"closed"->Business.Closed;"temporarily_closed"->Business.TemporarilyClosed;else->Business.Unknown},j.optBoolean("enabled",true),j.optString("source","OSM"),j.getString("updated_at"),j.getLong("version"),if(j.isNull("average_rating")) null else j.getDouble("average_rating"),j.optInt("rating_count"))
    }
    fun restaurant(r: Restaurant)=JSONObject().put("id",r.id).put("name",r.name).put("address",r.address).put("latitude",r.point?.latitude ?: JSONObject.NULL).put("longitude",r.point?.longitude ?: JSONObject.NULL).put("cuisines",JSONArray(r.cuisines.map(::cuisine))).put("opening_hours",r.openingHours ?: JSONObject.NULL).put("status",status(r.status)).put("enabled",r.enabled).put("source",r.source).put("updated_at",r.updatedAt).put("version",r.version).put("average_rating",r.averageRating ?: JSONObject.NULL).put("rating_count",r.ratingCount)
    fun status(s: Business)=when(s) {Business.Unknown->"unknown";Business.Operating->"operating";Business.Closed->"closed";Business.TemporarilyClosed->"temporarily_closed"}
    fun snapshot(j: JSONObject)=CandidateSnapshot(j.getString("id"),j.getString("token"),j.getLong("expires_at"),j.getJSONArray("cuisines").objects().map {CuisineCount(cuisine(it),it.getInt("count"))}.sortedBy {it.cuisine.id},j.getInt("restaurant_count"),j.getInt("unclassified_count"))
    fun snapshot(s: CandidateSnapshot)=JSONObject().put("id",s.id).put("token",s.token).put("expires_at",s.expiresAt).put("cuisines",JSONArray(s.cuisines.map {cuisine(it.cuisine).put("count",it.count)})).put("restaurant_count",s.restaurantCount).put("unclassified_count",s.unclassifiedCount)
    fun session(j: JSONObject,s: CandidateSnapshot)=DrawSession(j.getString("draw_id"),s,j.getString("cuisine_id"),s.cuisines.indexOfFirst {it.cuisine.id==j.getString("cuisine_id")},restaurant(j.getJSONObject("restaurant")),j.getInt("restaurant_count"),j.optBoolean("revealed"))
    fun session(s: DrawSession)=JSONObject().put("draw_id",s.drawId).put("cuisine_id",s.cuisineId).put("restaurant",restaurant(s.restaurant)).put("restaurant_count",s.restaurantCount).put("revealed",s.revealed)
    fun saved(s: SavedDraw)=JSONObject().put("request_id",s.request.id).put("snapshot",snapshot(s.request.snapshot)).put("mode",s.request.mode.name).put("previous_cuisine",s.request.previousCuisine ?: JSONObject.NULL).put("previous_restaurant",s.request.previousRestaurant ?: JSONObject.NULL).put("session",s.session?.let(::session) ?: JSONObject.NULL).toString()
    fun saved(raw: String): SavedDraw {
        val j=JSONObject(raw);val s=snapshot(j.getJSONObject("snapshot"))
        val r=DrawRequest(j.getString("request_id"),s,DrawMode.valueOf(j.getString("mode")),j.optString("previous_cuisine").takeUnless {it=="null" || it.isEmpty()},j.optString("previous_restaurant").takeUnless {it=="null" || it.isEmpty()})
        return SavedDraw(r,if(j.isNull("session")) null else session(j.getJSONObject("session"),s))
    }
    fun patch(p: EditPatch)=JSONObject().put("name",p.name.trim()).put("address",p.address.trim()).put("latitude",p.point?.latitude ?: JSONObject.NULL).put("longitude",p.point?.longitude ?: JSONObject.NULL).put("cuisine_ids",JSONArray(p.cuisineIds.sorted())).put("opening_hours",p.openingHours?.trim()?.takeIf {it.isNotEmpty()} ?: JSONObject.NULL).put("status",status(p.status))
}
