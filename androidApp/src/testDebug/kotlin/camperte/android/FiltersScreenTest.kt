package camperte.android

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import camperte.SearchArea
import camperte.SearchFilters
import camperte.readConfig
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class FiltersScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val chosenAreas = mutableListOf<SearchArea>()
    private var allowClicks = 0
    private var dismissals = 0
    private var settingsOpenings = 0
    private var resets = 0
    private var closes = 0

    private fun setContent(state: FiltersState) {
        var current by mutableStateOf(state)
        rule.setContent {
            CamperTeTheme(darkTheme = false) {
                FiltersScreen(
                    state = current,
                    onChange = { change -> current = current.copy(filters = change(current.filters)) },
                    onChooseArea = { chosenAreas += it },
                    onAllowLocation = { allowClicks++ },
                    onDismissLocation = { dismissals++ },
                    onOpenLocationSettings = { settingsOpenings++ },
                    onReset = { resets++ },
                    onClose = { closes++ },
                )
            }
        }
    }

    private fun state(locationDenied: Boolean = false, pendingDistanceKm: Int? = null) =
        FiltersState(SearchFilters.from(readConfig()), locationDenied, pendingDistanceKm)

    @Test
    fun stepsPrices() {
        setContent(state())

        rule.onNodeWithContentDescription("Aumenta Da").performClick()
        rule.onNodeWithContentDescription("Diminuisci A").performClick()

        rule.onNodeWithText("€ 16.000").assertIsDisplayed()
        rule.onNodeWithText("€ 34.000").assertIsDisplayed()
    }

    @Test
    fun stepsYearAndMileage() {
        setContent(state())

        rule.onNodeWithContentDescription("Aumenta Immatricolato dal").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Diminuisci Chilometri al massimo").performScrollTo().performClick()

        rule.onNodeWithText("2009").assertIsDisplayed()
        rule.onNodeWithText("140.000 km").assertIsDisplayed()
    }

    @Test
    fun choosesDistanceAndWholeItaly() {
        setContent(state())

        rule.onNodeWithText("100 km").performScrollTo().performClick()
        rule.onNodeWithText(string(R.string.filters_whole_italy)).performScrollTo().performClick()

        assertEquals(listOf(SearchArea.Nearby(100), SearchArea.WholeItaly), chosenAreas)
    }

    @Test
    fun togglesCamperTypes() {
        setContent(state())

        rule.onNodeWithText("Semintegrale").performScrollTo().assertIsOn().performClick()
        rule.onNodeWithText("Motorhome").performScrollTo().assertIsOff().performClick()

        rule.onNodeWithText("Semintegrale").assertIsOff()
        rule.onNodeWithText("Motorhome").assertIsOn()
    }

    @Test
    fun asksForLocationBeforeUsingDistance() {
        setContent(state(pendingDistanceKm = 150))

        rule.onNodeWithText(string(R.string.location_title)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.location_allow)).performClick()

        assertEquals(1, allowClicks)
    }

    @Test
    fun explainsDeniedLocationAndDisablesDistances() {
        setContent(state(locationDenied = true))

        rule.onNodeWithText(string(R.string.filters_location_off)).assertIsDisplayed()
        rule.onNodeWithText("50 km").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText(string(R.string.filters_enable_location)).performScrollTo().performClick()

        assertEquals(1, settingsOpenings)
    }

    @Test
    fun showButtonClosesAndResetResets() {
        setContent(state())

        rule.onNodeWithText(string(R.string.filters_show)).performScrollTo().performClick()
        rule.onNodeWithText(string(R.string.filters_reset)).performScrollTo().performClick()

        assertEquals(1, closes)
        assertEquals(1, resets)
    }

    @Test
    fun rendersInTheDarkTheme() {
        rule.setContent {
            CamperTeTheme(darkTheme = true) {
                FiltersScreen(state(), {}, {}, {}, {}, {}, {}, {})
            }
        }

        rule.onNodeWithText(string(R.string.filters)).assertIsDisplayed()
    }

    private fun string(id: Int) = RuntimeEnvironment.getApplication().getString(id)
}
