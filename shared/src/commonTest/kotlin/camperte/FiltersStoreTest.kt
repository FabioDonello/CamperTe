package camperte

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class FiltersStoreTest {

    private val defaults = SearchFilters.from(readConfig())

    @Test
    fun returnsDefaultsWhenNothingIsSaved() {
        assertEquals(defaults, FiltersStore(MapSettings(), defaults).load())
    }

    @Test
    fun savesAndLoadsFiltersWithArea() {
        val settings = MapSettings()
        val changed = defaults.stepMaxPrice(up = true).withArea(SearchArea.Nearby(100))

        FiltersStore(settings, defaults).save(changed)

        assertEquals(changed, FiltersStore(settings, defaults).load())
    }

    @Test
    fun fallsBackToDefaultsWhenSavedDataIsCorrupted() {
        val settings = MapSettings("search_filters" to "{not json")

        assertEquals(defaults, FiltersStore(settings, defaults).load())
    }
}
