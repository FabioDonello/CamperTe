package camperte.android

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import camperte.Listing
import camperte.SearchResult
import camperte.ShownListing
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
class ListingsScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private var refreshes = 0
    private var retries = 0
    private val opened = mutableListOf<String>()

    private val complete = ShownListing(
        listing = listing(
            id = "laika",
            title = "Mansardato Laika Ecovip",
            price = 28500,
            year = 2012,
            mileageKm = 98000,
            location = "Brescia",
            photoUrls = listOf("https://example.com/laika-1.jpg", "https://example.com/laika-2.jpg", "https://example.com/laika-3.jpg"),
        ),
        bonusWords = listOf("letto fisso", "garage"),
        isNew = true,
    )
    private val bare = ShownListing(listing(id = "elnagh", title = "Camper Elnagh Baron"), emptyList(), isNew = false)

    private fun setContent(result: SearchResult?, darkTheme: Boolean = false, isRefreshing: Boolean = false) {
        rule.setContent {
            CamperTeTheme(darkTheme = darkTheme) {
                ListingsScreen(
                    state = ListingsState(result, isRefreshing),
                    onRefresh = { refreshes++ },
                    onRetry = { retries++ },
                    onOpenListing = { opened += it },
                )
            }
        }
    }

    @Test
    fun showsLoadingMessageBeforeFirstResult() {
        setContent(result = null)

        rule.onNodeWithText(string(R.string.loading)).assertIsDisplayed()
    }

    @Test
    fun retryButtonRetriesWhenOffline() {
        setContent(SearchResult.NoConnection)

        rule.onNodeWithText(string(R.string.no_connection)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.retry)).performClick()

        assertEquals(1, retries)
    }

    @Test
    fun showsFriendlyMessageWhenNothingMatches() {
        setContent(found(emptyList()))

        rule.onNodeWithText(string(R.string.no_results)).assertIsDisplayed()
        rule.onNodeWithText("0 camper").assertIsDisplayed()
    }

    @Test
    fun showsHeaderCountersAndUnavailableSources() {
        setContent(found(listOf(complete, bare), unavailableSources = listOf("Subito")))

        rule.onNodeWithText("2 camper").assertIsDisplayed()
        rule.onNodeWithText("1 nuovo").assertIsDisplayed()
        rule.onNodeWithText("Subito non è raggiungibile al momento").assertIsDisplayed()
    }

    @Test
    fun hidesNewCounterWhenNothingIsNew() {
        setContent(found(listOf(bare)))

        rule.onAllNodesWithText("nuov", substring = true).assertCountEquals(0)
    }

    @Test
    fun showsListingDetails() {
        setContent(found(listOf(complete)))

        rule.onNodeWithText("€ 28.500").assertIsDisplayed()
        rule.onNodeWithText("Mansardato Laika Ecovip").assertIsDisplayed()
        rule.onNodeWithText("2012 · 98.000 km · Brescia").assertIsDisplayed()
        rule.onNodeWithText(string(R.string.new_badge)).assertIsDisplayed()
        rule.onNodeWithText("letto fisso").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("garage").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun showsPlaceholderWhenPhotoIsMissing() {
        setContent(found(listOf(bare)))

        rule.onNodeWithText(string(R.string.photo_unavailable)).assertIsDisplayed()
        rule.onAllNodesWithText("€", substring = true).assertCountEquals(0)
    }

    @Test
    fun showsPhotoCounterAndMovesItWhenSwiping() {
        setContent(found(listOf(complete)))

        rule.onNodeWithContentDescription("Foto 1 di 3").assertIsDisplayed()
        rule.onNodeWithText("1 / 3", useUnmergedTree = true).assertExists()

        rule.onAllNodesWithContentDescription("Mansardato Laika Ecovip")[0].performTouchInput { swipeLeft() }

        rule.onNodeWithContentDescription("Foto 2 di 3").assertIsDisplayed()
    }

    @Test
    fun hidesPhotoCounterForSinglePhoto() {
        setContent(found(listOf(ShownListing(listing(id = "one", title = "Uno", photoUrls = listOf("https://example.com/1.jpg")), emptyList(), false))))

        rule.onAllNodesWithContentDescription("Foto", substring = true).assertCountEquals(0)
    }

    @Test
    fun openListingButtonOpensItsUrl() {
        setContent(found(listOf(complete, bare)))

        rule.onAllNodesWithText(string(R.string.open_listing))[0].performScrollTo().performClick()

        assertEquals(listOf("https://example.com/laika"), opened)
    }

    @Test
    fun rendersInTheDarkTheme() {
        setContent(found(listOf(complete)), darkTheme = true)

        rule.onNodeWithText("Mansardato Laika Ecovip").assertIsDisplayed()
    }

    private fun found(listings: List<ShownListing>, unavailableSources: List<String> = emptyList()) =
        SearchResult.Found(listings, Instant.fromEpochMilliseconds(1_800_000_000_000), unavailableSources)

    private fun listing(
        id: String,
        title: String,
        price: Int? = null,
        year: Int? = null,
        mileageKm: Int? = null,
        location: String? = null,
        photoUrls: List<String> = emptyList(),
    ) = Listing(
        id = id,
        source = "Test",
        title = title,
        price = price,
        year = year,
        mileageKm = mileageKm,
        camperType = null,
        berths = null,
        location = location,
        region = null,
        shortDescription = null,
        photoUrls = photoUrls,
        url = "https://example.com/$id",
    )

    private fun string(id: Int) = RuntimeEnvironment.getApplication().getString(id)
}
