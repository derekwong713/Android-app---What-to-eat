package hk.whatoeat.data.location
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import hk.whatoeat.core.model.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
class FusedLocationRepository(private val context: Context,private val time: TimeSource): LocationRepository {
    override suspend fun locate(): LocationFix {
        val fine=ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
        val coarse=ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
        if(!fine && !coarse) throw AppFailure(Failure.Permission)
        if(!LocationManagerCompat.isLocationEnabled(context.getSystemService(LocationManager::class.java))) throw AppFailure(Failure.LocationDisabled)
        val cancellation=CancellationTokenSource()
        try {
            val request=CurrentLocationRequest.Builder().setPriority(if(fine) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY).setMaxUpdateAgeMillis(0).setDurationMillis(12000).build()
            val location=withTimeout(15000) {LocationServices.getFusedLocationProviderClient(context).getCurrentLocation(request,cancellation.token).await()} ?: throw AppFailure(Failure.LocationUnavailable)
            val point=Point(location.latitude,location.longitude)
            if(!point.usable || !location.hasAccuracy() || !location.accuracy.isFinite()) throw AppFailure(Failure.LocationUnavailable)
            if(time.now()-location.time !in 0..30000L) throw AppFailure(Failure.StaleLocation)
            return LocationFix(point,location.time,location.accuracy,!fine)
        } catch(e: TimeoutCancellationException) {throw AppFailure(Failure.LocationTimeout)}
        catch(e: SecurityException) {throw AppFailure(Failure.Permission)}
        catch(e: com.google.android.gms.common.api.ApiException) {throw AppFailure(Failure.LocationUnavailable)}
        finally {cancellation.cancel()}
    }
}
