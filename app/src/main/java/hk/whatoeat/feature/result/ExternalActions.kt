package hk.whatoeat.feature.result

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import hk.whatoeat.core.model.Restaurant

object ExternalActions {
    fun navigationUrl(r: Restaurant): String? {
        val destination = r.point?.let { "${it.latitude},${it.longitude}" } ?: r.address ?: r.name ?: return null
        val b = Uri.parse("https://www.google.com/maps/dir/").buildUpon().appendQueryParameter("api","1")
            .appendQueryParameter("destination",destination)
        if(r.provider=="google") b.appendQueryParameter("destination_place_id",r.placeId)
        return b.build().toString()
    }
    fun navigate(context: Context,r: Restaurant): Boolean {
        val url=navigationUrl(r) ?: return false
        if(r.provider=="google") {
            try { context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)).setPackage("com.google.android.apps.maps"));return true }
            catch(_: ActivityNotFoundException) { }
            catch(_: SecurityException) { }
        }
        return open(context,url)
    }
    fun open(context: Context,url: String): Boolean = try {
        context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)));true
    } catch(_: ActivityNotFoundException) { false } catch(_: SecurityException) { false }
}
