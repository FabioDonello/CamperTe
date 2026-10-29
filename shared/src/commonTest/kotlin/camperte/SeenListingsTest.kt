package camperte

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class SeenListingsTest {

    private val start = Instant.fromEpochMilliseconds(1_800_000_000_000)

    @Test
    fun everyListingIsNewOnFirstLaunch() {
        val seen = SeenListings(MapSettings())

        assertEquals(setOf("a", "b"), seen.recordShown(listOf("a", "b"), start))
    }

    @Test
    fun onlyListingsAppearedMeanwhileAreNewOnNextDay() {
        val settings = MapSettings()
        SeenListings(settings).recordShown(listOf("a", "b"), start)

        val newIds = SeenListings(settings).recordShown(listOf("a", "b", "c"), start + 1.days)

        assertEquals(setOf("c"), newIds)
    }

    @Test
    fun listingsStayNewForTwelveHoursAfterFirstShown() {
        val seen = SeenListings(MapSettings())
        seen.recordShown(listOf("a"), start)

        assertEquals(setOf("a"), seen.recordShown(listOf("a"), start + 11.hours))
        assertEquals(emptySet(), seen.recordShown(listOf("a"), start + 12.hours))
    }

    @Test
    fun forgetsListingsNotShownForNinetyDays() {
        val seen = SeenListings(MapSettings())
        seen.recordShown(listOf("gone", "kept"), start)
        seen.recordShown(listOf("kept"), start + 60.days)

        val newIds = seen.recordShown(listOf("gone", "kept"), start + 91.days)

        assertEquals(setOf("gone"), newIds)
    }

    @Test
    fun keepsListingsShownWithinNinetyDays() {
        val seen = SeenListings(MapSettings())
        seen.recordShown(listOf("a"), start)

        assertEquals(emptySet(), seen.recordShown(listOf("a"), start + 90.days))
    }

    @Test
    fun treatsCorruptedMemoryAsEmpty() {
        val seen = SeenListings(MapSettings("seen_listings" to "{not json"))

        assertEquals(setOf("a"), seen.recordShown(listOf("a"), start))
    }
}
