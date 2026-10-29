package camperte

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeoTest {

    private val milano = GeoPoint(45.4642, 9.1900)
    private val bologna = GeoPoint(44.4949, 11.3426)
    private val modena = GeoPoint(44.6471, 10.9252)

    @Test
    fun measuresDistanceBetweenCities() {
        assertTrue(abs(distanceKm(milano, bologna) - 201) < 3)
        assertEquals(0.0, distanceKm(milano, milano))
    }

    @Test
    fun findsRegionsNearAPointClosestFirst() {
        val near = regionsNear(modena, 50)

        assertEquals("Emilia-Romagna", near.first())
        assertTrue("Sicilia" !in near)
    }

    @Test
    fun limitsTheNumberOfRegions() {
        assertEquals(MAX_NEARBY_REGIONS, regionsNear(modena, 300).size)
    }

    @Test
    fun findsNoRegionsOutsideItaly() {
        assertEquals(emptyList(), regionsNear(GeoPoint(48.8566, 2.3522), 100))
    }

    @Test
    fun usesAppNamesForRegionsWrittenDifferently() {
        assertEquals("Emilia-Romagna", italianRegionName("Emilia Romagna"))
        assertEquals("Trentino-Alto Adige", italianRegionName("Trentino Alto Adige"))
        assertEquals("Valle d'Aosta", italianRegionName("valle d aosta"))
        assertEquals("Estero", italianRegionName(" Estero "))
    }
}
