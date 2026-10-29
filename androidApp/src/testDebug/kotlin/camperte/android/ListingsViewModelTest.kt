package camperte.android

import android.os.Looper
import camperte.SearchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ListingsViewModelTest {

    @Test
    fun loadsListingsWhenCreated() {
        val viewModel = ListingsViewModel()
        idle()

        assertEquals(3, (viewModel.state.result as SearchResult.Found).listings.size)
        assertFalse(viewModel.state.isRefreshing)
    }

    @Test
    fun refreshReloadsAndStopsRefreshing() {
        val viewModel = ListingsViewModel()
        idle()

        viewModel.refresh()
        idle()

        assertEquals(3, (viewModel.state.result as SearchResult.Found).listings.size)
        assertFalse(viewModel.state.isRefreshing)
    }

    @Test
    fun retryReloads() {
        val viewModel = ListingsViewModel()
        idle()

        viewModel.retry()
        idle()

        assertEquals(3, (viewModel.state.result as SearchResult.Found).listings.size)
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()
}
