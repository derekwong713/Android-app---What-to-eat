package hk.whatoeat.feature.result
import android.content.*
import android.net.Uri
import hk.whatoeat.core.model.Restaurant
object ExternalActions {
 fun restaurantUrl(r: Restaurant): String {
  val source=r.mapsUrl?.let {Uri.parse(it)}
  if(source!=null && source.scheme=="https" && source.host in setOf("www.google.com","google.com","maps.google.com") && source.userInfo==null && source.port==-1 && (source.path=="/maps" || source.path.orEmpty().startsWith("/maps/"))) return source.toString()
  return Uri.parse("https://www.google.com/maps/search/").buildUpon().appendQueryParameter("api","1").appendQueryParameter("query",listOf(r.name,r.address).filter {it.isNotBlank()}.joinToString(" ")).build().toString()
 }

 fun navigationUrl(r: Restaurant): String? {
  val destination=r.point?.takeIf {it.usable}?.let {"${it.latitude},${it.longitude}"} ?: r.address.takeIf {it.isNotBlank()}?.let {r.name+" "+it} ?: return null
  return Uri.parse("https://www.google.com/maps/dir/").buildUpon().appendQueryParameter("api","1").appendQueryParameter("destination",destination).build().toString()
 }
 fun navigate(c: Context,r: Restaurant): Boolean {val url=navigationUrl(r) ?: return false;return try {c.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)).putExtra(Intent.EXTRA_TITLE,r.name));true} catch(_: ActivityNotFoundException) {false} catch(_: SecurityException) {false}}
 fun open(c: Context,url: String): Boolean=try {val u=Uri.parse(url);if(u.scheme!="https") false else {c.startActivity(Intent(Intent.ACTION_VIEW,u));true}} catch(_: ActivityNotFoundException) {false} catch(_: SecurityException) {false}
}
