package camperte

import camperte.sources.Source
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class CamperRepositoryTest {

    private val now = Instant.fromEpochMilliseconds(1_800_000_000_000)
    private val modena = GeoPoint(44.6471, 10.9252)
    private val fixedClock = object : Clock {
        override fun now() = now
    }

    @Test
    fun returnsFilteredAndSortedListingsWithBonusWords() = runTest {
        val source = FakeSource(
            "Uno",
            listing("expensive", price = 30000),
            listing("cheap", price = 20000),
            listing("bonus", price = 34000, title = "Mansardato con garage"),
            listing("excluded", price = 25000, title = "Cerco camper"),
        )

        val result = assertIs<SearchResult.Found>(repository(source).loadListings(null))

        assertEquals(listOf("bonus", "cheap", "expensive"), result.listings.map { it.listing.id })
        assertEquals(listOf("garage"), result.listings.first().bonusWords)
        assertEquals(now, result.updatedAt)
        assertEquals(emptyList(), result.unavailableSources)
        assertEquals(3, result.newCount)
    }

    @Test
    fun marksOnlyListingsAppearedSinceLastOpeningAsNewAndShowsThemFirst() = runTest {
        val settings = MapSettings()
        var clockTime = now
        val clock = object : Clock {
            override fun now() = clockTime
        }
        CamperRepository(listOf(FakeSource("Uno", listing("old", price = 20000))), readConfig(), clock, settings)
            .loadListings(null)
        clockTime = now + 1.days
        val source = FakeSource("Uno", listing("old", price = 20000), listing("fresh", price = 30000))

        val result = assertIs<SearchResult.Found>(
            CamperRepository(listOf(source), readConfig(), clock, settings).loadListings(null)
        )

        assertEquals(listOf("fresh" to true, "old" to false), result.listings.map { it.listing.id to it.isNew })
        assertEquals(1, result.newCount)
    }

    @Test
    fun keepsOtherSourcesWhenOneFails() = runTest {
        val working = FakeSource("Uno", listing("a"))
        val broken = FakeSource("Subito", error = IllegalStateException("Unexpected page"))

        val result = assertIs<SearchResult.Found>(repository(broken, working).loadListings(null))

        assertEquals(listOf("a"), result.listings.map { it.listing.id })
        assertEquals(listOf("Subito"), result.unavailableSources)
    }

    @Test
    fun reportsSourceUnreachableWhenOnlySomeConnectionsFail() = runTest {
        val working = FakeSource("Uno", listing("a"))
        val offline = FakeSource("Subito", error = IOException("Timeout"))

        val result = assertIs<SearchResult.Found>(repository(offline, working).loadListings(null))

        assertEquals(listOf("Subito"), result.unavailableSources)
    }

    @Test
    fun reportsNoConnectionWhenEverySourceHasConnectionError() = runTest {
        val first = FakeSource("Uno", error = IOException("Offline"))
        val second = FakeSource("Due", error = IOException("Offline"))

        assertEquals(SearchResult.NoConnection, repository(first, second).loadListings(null))
    }

    @Test
    fun reportsUnavailableSourcesWhenEverySourceFailsForOtherReasons() = runTest {
        val offline = FakeSource("Uno", error = IOException("Offline"))
        val blocked = FakeSource("Subito", error = IllegalStateException("Blocked"))

        val result = assertIs<SearchResult.Found>(repository(offline, blocked).loadListings(null))

        assertEquals(emptyList(), result.listings)
        assertEquals(listOf("Uno", "Subito"), result.unavailableSources)
    }

    @Test
    fun returnsEmptyResultWithoutSources() = runTest {
        val result = assertIs<SearchResult.Found>(repository().loadListings(null))

        assertEquals(emptyList(), result.listings)
    }

    @Test
    fun queriesSourcesInParallel() = runTest {
        val first = FakeSource("Uno", listing("a"), delayMillis = 1_000)
        val second = FakeSource("Due", listing("b"), delayMillis = 1_000)

        repository(first, second).loadListings(null)

        assertEquals(1_000, currentTime)
    }

    @Test
    fun appliesSavedFilters() = runTest {
        val source = FakeSource("Uno", listing("cheap", price = 20000), listing("dear", price = 30000))
        val repository = repository(source)

        repository.saveFilters(repository.currentFilters().copy(maxPrice = 25000))
        val result = assertIs<SearchResult.Found>(repository.loadListings(null))

        assertEquals(listOf("cheap"), result.listings.map { it.listing.id })
    }

    @Test
    fun searchesConfiguredRegionsByDefault() = runTest {
        val source = FakeSource("Uno")

        repository(source).loadListings(modena)

        assertEquals(listOf("Lombardia", "Emilia-Romagna", "Veneto"), source.searchedRegions)
    }

    @Test
    fun searchesRegionsNearThePhoneAndDropsFarListings() = runTest {
        val source = FakeSource(
            "Uno",
            listing("near", position = GeoPoint(44.70, 10.63), region = "Emilia-Romagna"),
            listing("far", position = GeoPoint(45.46, 9.19), region = "Lombardia"),
            listing("unknown"),
        )
        val repository = repository(source)
        repository.saveFilters(repository.currentFilters().withArea(SearchArea.Nearby(50)))

        val result = assertIs<SearchResult.Found>(repository.loadListings(modena))

        assertEquals("Emilia-Romagna", source.searchedRegions.first())
        assertEquals(listOf("near", "unknown"), result.listings.map { it.listing.id })
        assertEquals(24, result.listings.first().distanceKm)
    }

    @Test
    fun keepsListingsWithoutCoordinatesOnlyInNearbyRegions() = runTest {
        val source = FakeSource(
            "Uno",
            listing("nearby-region", region = "Emilia-Romagna"),
            listing("far-region", region = "Sicilia"),
        )
        val repository = repository(source)
        repository.saveFilters(repository.currentFilters().withArea(SearchArea.Nearby(50)))

        val result = assertIs<SearchResult.Found>(repository.loadListings(modena))

        assertEquals(listOf("nearby-region"), result.listings.map { it.listing.id })
    }

    @Test
    fun fallsBackToConfiguredRegionsWithoutPosition() = runTest {
        val source = FakeSource("Uno")
        val repository = repository(source)
        repository.saveFilters(repository.currentFilters().withArea(SearchArea.Nearby(50)))

        repository.loadListings(null)

        assertEquals(listOf("Lombardia", "Emilia-Romagna", "Veneto"), source.searchedRegions)
    }

    @Test
    fun searchesWholeItalyWithoutRegions() = runTest {
        val source = FakeSource("Uno", listing("sicily", region = "Sicilia"))
        val repository = repository(source)
        repository.saveFilters(repository.currentFilters().withArea(SearchArea.WholeItaly))

        val result = assertIs<SearchResult.Found>(repository.loadListings(null))

        assertEquals(emptyList(), source.searchedRegions)
        assertEquals(listOf("sicily"), result.listings.map { it.listing.id })
    }

    private fun repository(vararg sources: Source) = CamperRepository(sources.toList(), readConfig(), fixedClock, MapSettings())

    private fun listing(
        id: String,
        price: Int? = null,
        title: String = "Camper",
        position: GeoPoint? = null,
        region: String? = null,
    ) = Listing(
        id = id,
        source = "Test",
        title = title,
        price = price,
        year = null,
        mileageKm = null,
        camperType = null,
        berths = null,
        location = null,
        region = region,
        shortDescription = null,
        photoUrls = emptyList(),
        url = "https://example.com/$id",
        position = position,
    )

    private class FakeSource(
        override val name: String,
        private vararg val listings: Listing,
        private val error: Exception? = null,
        private val delayMillis: Long = 0,
    ) : Source {
        var searchedRegions: List<String> = emptyList()

        override suspend fun search(config: Config): List<Listing> {
            searchedRegions = config.regions
            delay(delayMillis)
            error?.let { throw it }
            return listings.toList()
        }
    }
}
