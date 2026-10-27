package hk.whatoeat.feature.result
import android.content.*
import android.net.Uri
import hk.whatoeat.core.model.Restaurant
object ExternalActions {
 fun navigationUrl(r: Restaurant): String? {
  val destination=r.point?.takeIf {it.usable}?.let {"${it.latitude},${it.longitude}"} ?: r.address.takeIf {it.isNotBlank()}?.let {r.name+" "+it} ?: return null
  return Uri.parse("https://www.google.com/maps/dir/").buildUpon().appendQueryParameter("api","1").appendQueryParameter("destination",destination).build().toString()
 }
 fun navigate(c: Context,r: Restaurant): Boolean {val url=navigationUrl(r) ?: return false;return try {c.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)).putExtra(Intent.EXTRA_TITLE,r.name));true} catch(_: ActivityNotFoundException) {false} catch(_: SecurityException) {false}}
 fun open(c: Context,url: String): Boolean=try {val u=Uri.parse(url);if(u.scheme!="https") false else {c.startActivity(Intent(Intent.ACTION_VIEW,u));true}} catch(_: ActivityNotFoundException) {false} catch(_: SecurityException) {false}
}
