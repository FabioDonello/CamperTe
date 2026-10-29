package camperte

import kotlin.test.Test
import kotlin.test.assertEquals

class SearchFiltersTest {

    private val defaults = SearchFilters.from(readConfig())

    @Test
    fun startsFromConfig() {
        assertEquals(15000, defaults.minPrice)
        assertEquals(35000, defaults.maxPrice)
        assertEquals(2008, defaults.minYear)
        assertEquals(150000, defaults.maxMileageKm)
        assertEquals(listOf("mansardato", "semintegrale", "furgonato"), defaults.camperTypes)
        assertEquals(SearchArea.ConfiguredRegions, defaults.area)
    }

    @Test
    fun stepsPricesByAThousandKeepingMinBelowMax() {
        assertEquals(16000, defaults.stepMinPrice(up = true).minPrice)
        assertEquals(34000, defaults.stepMaxPrice(up = false).maxPrice)
        assertEquals(35000, defaults.copy(minPrice = 35000).stepMinPrice(up = true).minPrice)
        assertEquals(15000, defaults.copy(maxPrice = 15000).stepMaxPrice(up = false).maxPrice)
        assertEquals(0, defaults.copy(minPrice = 0).stepMinPrice(up = false).minPrice)
    }

    @Test
    fun stepsYearAndMileage() {
        assertEquals(2009, defaults.stepMinYear(up = true).minYear)
        assertEquals(140000, defaults.stepMaxMileage(up = false).maxMileageKm)
        assertEquals(10000, defaults.copy(maxMileageKm = 10000).stepMaxMileage(up = false).maxMileageKm)
    }

    @Test
    fun togglesCamperTypes() {
        assertEquals(listOf("mansardato", "furgonato"), defaults.toggleCamperType("semintegrale").camperTypes)
        assertEquals(
            listOf("mansardato", "semintegrale", "furgonato", "motorhome"),
            defaults.toggleCamperType("motorhome").camperTypes,
        )
    }

    @Test
    fun formatsValuesForDisplay() {
        assertEquals("€ 15.000", defaults.minPriceText)
        assertEquals("€ 35.000", defaults.maxPriceText)
        assertEquals("150.000 km", defaults.maxMileageText)
    }

    @Test
    fun summarisesFilters() {
        assertEquals(
            "€ 15.000 – 35.000 · Lombardia, Emilia-Romagna, Veneto · dal 2008 · fino a 150.000 km · mansardato, semintegrale, furgonato",
            defaults.summaryText(listOf("Lombardia", "Emilia-Romagna", "Veneto"), hasPosition = false),
        )
    }

    @Test
    fun summarisesAreaByPositionAvailability() {
        val nearby = defaults.withArea(SearchArea.Nearby(150))

        assertEquals("entro 150 km", nearby.summaryText(listOf("Veneto"), hasPosition = true).split(" · ")[1])
        assertEquals("Veneto", nearby.summaryText(listOf("Veneto"), hasPosition = false).split(" · ")[1])
        assertEquals("tutta Italia", defaults.withArea(SearchArea.WholeItaly).summaryText(listOf("Veneto"), false).split(" · ")[1])
    }

    @Test
    fun summarisesEmptyTypesAsAll() {
        val noTypes = defaults.copy(camperTypes = emptyList())

        assertEquals("tutte le tipologie", noTypes.summaryText(emptyList(), false).split(" · ").last())
    }
}
