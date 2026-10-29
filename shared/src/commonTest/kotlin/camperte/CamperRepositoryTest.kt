package camperte

import camperte.sources.Source
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlin.time.Instant

class CamperRepositoryTest {

    private val now = Instant.fromEpochMilliseconds(1_800_000_000_000)
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

        val result = assertIs<SearchResult.Found>(repository(source).loadListings())

        assertEquals(listOf("bonus", "cheap", "expensive"), result.listings.map { it.listing.id })
        assertEquals(listOf("garage"), result.listings.first().bonusWords)
        assertEquals(now, result.updatedAt)
        assertEquals(emptyList(), result.unavailableSources)
        assertEquals(0, result.newCount)
    }

    @Test
    fun keepsOtherSourcesWhenOneFails() = runTest {
        val working = FakeSource("Uno", listing("a"))
        val broken = FakeSource("Subito", error = IllegalStateException("Unexpected page"))

        val result = assertIs<SearchResult.Found>(repository(broken, working).loadListings())

        assertEquals(listOf("a"), result.listings.map { it.listing.id })
        assertEquals(listOf("Subito"), result.unavailableSources)
    }

    @Test
    fun reportsSourceUnreachableWhenOnlySomeConnectionsFail() = runTest {
        val working = FakeSource("Uno", listing("a"))
        val offline = FakeSource("Subito", error = IOException("Timeout"))

        val result = assertIs<SearchResult.Found>(repository(offline, working).loadListings())

        assertEquals(listOf("Subito"), result.unavailableSources)
    }

    @Test
    fun reportsNoConnectionWhenEverySourceHasConnectionError() = runTest {
        val first = FakeSource("Uno", error = IOException("Offline"))
        val second = FakeSource("Due", error = IOException("Offline"))

        assertEquals(SearchResult.NoConnection, repository(first, second).loadListings())
    }

    @Test
    fun reportsUnavailableSourcesWhenEverySourceFailsForOtherReasons() = runTest {
        val offline = FakeSource("Uno", error = IOException("Offline"))
        val blocked = FakeSource("Subito", error = IllegalStateException("Blocked"))

        val result = assertIs<SearchResult.Found>(repository(offline, blocked).loadListings())

        assertEquals(emptyList(), result.listings)
        assertEquals(listOf("Uno", "Subito"), result.unavailableSources)
    }

    @Test
    fun returnsEmptyResultWithoutSources() = runTest {
        val result = assertIs<SearchResult.Found>(repository().loadListings())

        assertEquals(emptyList(), result.listings)
    }

    @Test
    fun queriesSourcesInParallel() = runTest {
        val first = FakeSource("Uno", listing("a"), delayMillis = 1_000)
        val second = FakeSource("Due", listing("b"), delayMillis = 1_000)

        repository(first, second).loadListings()

        assertEquals(1_000, currentTime)
    }

    private fun repository(vararg sources: Source) = CamperRepository(sources.toList(), readConfig(), fixedClock)

    private fun listing(id: String, price: Int? = null, title: String = "Camper") = Listing(
        id = id,
        source = "Test",
        title = title,
        price = price,
        year = null,
        mileageKm = null,
        camperType = null,
        berths = null,
        location = null,
        region = null,
        shortDescription = null,
        photoUrl = null,
        url = "https://example.com/$id",
    )

    private class FakeSource(
        override val name: String,
        private vararg val listings: Listing,
        private val error: Exception? = null,
        private val delayMillis: Long = 0,
    ) : Source {
        override suspend fun search(config: Config): List<Listing> {
            delay(delayMillis)
            error?.let { throw it }
            return listings.toList()
        }
    }
}
