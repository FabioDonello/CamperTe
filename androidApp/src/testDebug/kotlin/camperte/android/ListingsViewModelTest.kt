package camperte.android

import android.Manifest
import android.os.Looper
import camperte.CamperRepository
import camperte.Config
import camperte.Listing
import camperte.SearchArea
import camperte.SearchResult
import camperte.readConfig
import camperte.sources.Source
import com.russhwolf.settings.MapSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import kotlin.time.Clock

@RunWith(RobolectricTestRunner::class)
class ListingsViewModelTest {

    private val source = CountingSource()
    private val repository = CamperRepository(listOf(source), readConfig(), Clock.System, MapSettings())

    @Test
    fun loadsListingsWhenCreated() {
        val viewModel = viewModel()
        idle()

        assertEquals(1, (viewModel.state.result as SearchResult.Found).listings.size)
        assertFalse(viewModel.state.isRefreshing)
        assertEquals(1, source.searches)
        assertTrue(viewModel.state.filtersSummary.startsWith("€ 15.000 – 35.000 · Lombardia"))
    }

    @Test
    fun refreshSearchesAgainAndStopsRefreshing() {
        val viewModel = viewModel()
        idle()

        viewModel.refresh()
        idle()

        assertEquals(2, source.searches)
        assertFalse(viewModel.state.isRefreshing)
    }

    @Test
    fun retrySearchesAgain() {
        val viewModel = viewModel()
        idle()

        viewModel.retry()
        idle()

        assertEquals(2, source.searches)
    }

    @Test
    fun closingChangedFiltersSavesAndSearchesAgain() {
        val viewModel = viewModel()
        idle()

        viewModel.openFilters()
        viewModel.editFilters { it.stepMaxPrice(up = false) }
        viewModel.closeFilters()
        idle()

        assertNull(viewModel.filtersState)
        assertEquals(34000, repository.currentFilters().maxPrice)
        assertEquals(2, source.searches)
        assertTrue(viewModel.state.filtersSummary.startsWith("€ 15.000 – 34.000"))
    }

    @Test
    fun closingUnchangedFiltersDoesNotSearchAgain() {
        val viewModel = viewModel()
        idle()

        viewModel.openFilters()
        viewModel.closeFilters()
        idle()

        assertEquals(1, source.searches)
    }

    @Test
    fun asksForLocationBeforeChoosingDistance() {
        val viewModel = viewModel()
        viewModel.openFilters()

        viewModel.chooseArea(SearchArea.Nearby(100))

        assertEquals(100, viewModel.filtersState?.pendingDistanceKm)
        assertEquals(SearchArea.ConfiguredRegions, viewModel.filtersState?.filters?.area)
    }

    @Test
    fun grantedLocationAppliesPendingDistance() {
        val viewModel = viewModel()
        viewModel.openFilters()
        viewModel.chooseArea(SearchArea.Nearby(100))

        viewModel.onLocationPermissionResult(granted = true)

        assertEquals(SearchArea.Nearby(100), viewModel.filtersState?.filters?.area)
        assertNull(viewModel.filtersState?.pendingDistanceKm)
    }

    @Test
    fun deniedLocationIsRemembered() {
        val viewModel = viewModel()
        viewModel.openFilters()
        viewModel.chooseArea(SearchArea.Nearby(100))

        viewModel.onLocationPermissionResult(granted = false)

        assertTrue(viewModel.filtersState?.locationDenied == true)
        assertEquals(SearchArea.ConfiguredRegions, viewModel.filtersState?.filters?.area)
    }

    @Test
    fun choosesDistanceDirectlyWhenLocationIsAllowed() {
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        val viewModel = viewModel()
        viewModel.openFilters()

        viewModel.chooseArea(SearchArea.Nearby(50))

        assertEquals(SearchArea.Nearby(50), viewModel.filtersState?.filters?.area)
    }

    @Test
    fun resetRestoresDefaults() {
        val viewModel = viewModel()
        viewModel.openFilters()
        viewModel.editFilters { it.stepMinYear(up = true).withArea(SearchArea.WholeItaly) }

        viewModel.resetFilters()

        assertEquals(repository.defaultFilters, viewModel.filtersState?.filters)
    }

    private fun viewModel() = ListingsViewModel(repository, LocationProvider(RuntimeEnvironment.getApplication()))

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private class CountingSource : Source {
        override val name = "Test"
        var searches = 0

        override suspend fun search(config: Config): List<Listing> {
            searches++
            return listOf(
                Listing(
                    id = "test-1",
                    source = name,
                    title = "Camper",
                    price = 20000,
                    year = null,
                    mileageKm = null,
                    camperType = null,
                    berths = null,
                    location = null,
                    region = null,
                    shortDescription = null,
                    photoUrls = emptyList(),
                    url = "https://example.com/test-1",
                    position = null,
                ),
            )
        }
    }
}
