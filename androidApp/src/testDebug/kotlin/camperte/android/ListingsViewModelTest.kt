package camperte.android

import android.os.Looper
import camperte.CamperRepository
import camperte.Config
import camperte.Listing
import camperte.SearchResult
import camperte.readConfig
import camperte.sources.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import kotlin.time.Clock

@RunWith(RobolectricTestRunner::class)
class ListingsViewModelTest {

    private val source = CountingSource()

    @Test
    fun loadsListingsWhenCreated() {
        val viewModel = viewModel()
        idle()

        assertEquals(1, (viewModel.state.result as SearchResult.Found).listings.size)
        assertFalse(viewModel.state.isRefreshing)
        assertEquals(1, source.searches)
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
        assertEquals(1, (viewModel.state.result as SearchResult.Found).listings.size)
    }

    private fun viewModel() = ListingsViewModel(CamperRepository(listOf(source), readConfig(), Clock.System))

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
                    photoUrl = null,
                    url = "https://example.com/test-1",
                ),
            )
        }
    }
}
