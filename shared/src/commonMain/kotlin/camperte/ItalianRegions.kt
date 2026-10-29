package camperte

private data class Region(
    val name: String,
    val minLatitude: Double,
    val maxLatitude: Double,
    val minLongitude: Double,
    val maxLongitude: Double,
) {
    fun distanceKmFrom(point: GeoPoint): Double = distanceKm(
        point,
        GeoPoint(point.latitude.coerceIn(minLatitude, maxLatitude), point.longitude.coerceIn(minLongitude, maxLongitude)),
    )
}

private val italianRegions = listOf(
    Region("Valle d'Aosta", 45.47, 45.99, 6.80, 7.94),
    Region("Piemonte", 44.06, 46.46, 6.63, 9.21),
    Region("Liguria", 43.78, 44.68, 7.49, 10.07),
    Region("Lombardia", 44.68, 46.64, 8.50, 11.43),
    Region("Trentino-Alto Adige", 45.67, 47.09, 10.38, 12.48),
    Region("Veneto", 44.79, 46.68, 10.62, 13.10),
    Region("Friuli-Venezia Giulia", 45.58, 46.65, 12.32, 13.92),
    Region("Emilia-Romagna", 43.73, 45.14, 9.20, 12.76),
    Region("Toscana", 42.24, 44.47, 9.69, 12.37),
    Region("Umbria", 42.36, 43.62, 11.89, 13.26),
    Region("Marche", 42.69, 43.97, 12.19, 13.92),
    Region("Lazio", 40.78, 42.84, 11.45, 14.03),
    Region("Abruzzo", 41.68, 42.90, 13.02, 14.79),
    Region("Molise", 41.36, 42.07, 13.94, 15.16),
    Region("Campania", 39.99, 41.51, 13.76, 15.81),
    Region("Puglia", 39.79, 42.23, 14.94, 18.52),
    Region("Basilicata", 39.90, 41.14, 15.34, 16.87),
    Region("Calabria", 37.92, 40.14, 15.63, 17.21),
    Region("Sicilia", 36.64, 38.82, 12.42, 15.65),
    Region("Sardegna", 38.86, 41.31, 8.13, 9.83),
)

internal const val MAX_NEARBY_REGIONS = 4

internal fun regionsNear(point: GeoPoint, maxDistanceKm: Int): List<String> =
    italianRegions
        .map { it to it.distanceKmFrom(point) }
        .filter { (_, distance) -> distance <= maxDistanceKm }
        .sortedBy { (_, distance) -> distance }
        .take(MAX_NEARBY_REGIONS)
        .map { (region, _) -> region.name }
