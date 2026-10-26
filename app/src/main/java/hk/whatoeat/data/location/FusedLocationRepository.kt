package hk.whatoeat.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import hk.whatoeat.core.model.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class FusedLocationRepository(private val context: Context, private val time: TimeSource) : LocationRepository {
    override suspend fun locate(): LocationFix {
        val coarse = ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val fine = ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!coarse && !fine) throw AppFailure(Failure.Permission)
        if (!LocationManagerCompat.isLocationEnabled(context.getSystemService(LocationManager::class.java))) throw AppFailure(Failure.LocationDisabled)
        val client = LocationServices.getFusedLocationProviderClient(context)
        fun android.location.Location.fix() = LocationFix(Point(latitude,longitude),this.time,accuracy,!fine)
        val last = client.lastLocation.await()?.fix()
        if (last != null && time.now() - last.timestamp <= 300000) return last
        val token = CancellationTokenSource()
        return try {
            val fresh = withTimeout(15000) {
                client.getCurrentLocation(if(fine) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY,token.token).await()
            }?.fix()
            if (fresh != null && time.now() - fresh.timestamp <= 300000) fresh
            else if(last != null) throw StaleFix(last) else throw AppFailure(Failure.LocationUnavailable)
        } catch (e: TimeoutCancellationException) {
            if (last != null) throw StaleFix(last) else throw AppFailure(Failure.LocationTimeout)
        } catch(e: SecurityException) {
            throw AppFailure(Failure.Permission)
        } finally { token.cancel() }
    }
}
class StaleFix(val fix: LocationFix) : Exception()
