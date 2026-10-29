package camperte.android

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import camperte.GeoPoint
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class LocationProvider(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    suspend fun currentPosition(): GeoPoint? {
        if (!hasPermission()) return null
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val provider = PROVIDERS.firstOrNull { manager.isProviderEnabled(it) } ?: return null
        val location = try {
            withTimeoutOrNull(TIMEOUT_MILLIS) { requestLocation(manager, provider) } ?: manager.lastKnown(provider)
        } catch (e: SecurityException) {
            Log.w(TAG, "Location permission revoked while reading $provider", e)
            null
        }
        return location?.let { GeoPoint(it.latitude, it.longitude) }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestLocation(manager: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { continuation ->
            val signal = CancellationSignal()
            continuation.invokeOnCancellation { signal.cancel() }
            LocationManagerCompat.getCurrentLocation(manager, provider, signal, ContextCompat.getMainExecutor(context)) {
                continuation.resume(it)
            }
        }

    @SuppressLint("MissingPermission")
    private fun LocationManager.lastKnown(provider: String): Location? = getLastKnownLocation(provider)

    private companion object {
        const val TAG = "LocationProvider"
        const val TIMEOUT_MILLIS = 10_000L
        val PROVIDERS = listOf("fused", LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
    }
}
