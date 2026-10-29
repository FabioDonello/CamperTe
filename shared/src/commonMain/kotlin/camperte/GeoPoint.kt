package camperte

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPoint(val latitude: Double, val longitude: Double)

private const val EARTH_RADIUS_KM = 6371.0

internal fun distanceKm(from: GeoPoint, to: GeoPoint): Double {
    val dLat = (to.latitude - from.latitude).toRadians()
    val dLon = (to.longitude - from.longitude).toRadians()
    val a = sin(dLat / 2).pow(2) + cos(from.latitude.toRadians()) * cos(to.latitude.toRadians()) * sin(dLon / 2).pow(2)
    return 2 * EARTH_RADIUS_KM * asin(sqrt(a))
}

private fun Double.toRadians(): Double = this * PI / 180
