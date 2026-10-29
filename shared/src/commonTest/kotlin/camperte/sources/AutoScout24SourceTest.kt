package camperte.sources

import camperte.readConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

class AutoScout24SourceTest {

    private val config = readConfig()

    @Test
    fun mapsListingFromPageData() {
        val listing = parseAutoScout24Page(AUTO_SCOUT_24_PAGE, camperType = "semintegrale").first()

        assertEquals("autoscout24-1dca81ed-f10a-4084-8c22-baa02905e383", listing.id)
        assertEquals("AutoScout24", listing.source)
        assertEquals("McLouis TANDY 636 MC LOUIS TANDY 636", listing.title)
        assertEquals(29990, listing.price)
        assertEquals(2008, listing.year)
        assertEquals(91500, listing.mileageKm)
        assertEquals("semintegrale", listing.camperType)
        assertEquals(4, listing.berths)
        assertEquals("Bologna (BO)", listing.location)
        assertEquals("Emilia-Romagna", listing.region)
        assertEquals(
            listOf(
                "https://prod.pictures.autoscout24.net/listing-images/" +
                    "1dca81ed-f10a-4084-8c22-baa02905e383_86743015-b4eb-4a68-a1de-11c43d3558d5.jpg/800x600.webp",
                "https://prod.pictures.autoscout24.net/listing-images/" +
                    "1dca81ed-f10a-4084-8c22-baa02905e383_0feda2a2-0809-4d55-a665-8193d43ef72a.jpg/800x600.webp",
            ),
            listing.photoUrls,
        )
        assertEquals(
            "https://www.autoscout24.it/annunci/mclouis-tandy-636-mc-louis-tandy-636-diesel-bianco-cat_ma20233-1dca81ed-f10a-4084-8c22-baa02905e383",
            listing.url,
        )
        assertNull(listing.position)
    }

    @Test
    fun leavesMissingDataEmpty() {
        val (_, ford, carthago) = parseAutoScout24Page(AUTO_SCOUT_24_PAGE, camperType = null)

        assertNull(ford.berths)
        assertNull(ford.camperType)
        assertEquals("Banchette (TO)", ford.location)
        assertEquals("Piemonte", ford.region)
        assertNull(carthago.year)
        assertEquals("Lombardia", carthago.region)
    }

    @Test
    fun skipsListingsOutsideItaly() {
        assertEquals(3, parseAutoScout24Page(AUTO_SCOUT_24_PAGE, camperType = null).size)
    }

    @Test
    fun failsWhenPageHasNoListingsData() {
        assertFailsWith<IllegalStateException> { parseAutoScout24Page("<html><body>Accesso negato</body></html>", null) }
    }

    @Test
    fun requestsOnePagePerCamperTypeWithFiltersAndRemovesDuplicates() = runTest {
        val requested = mutableListOf<Url>()
        val source = AutoScout24Source(client { url -> requested += url; respond(AUTO_SCOUT_24_PAGE) })

        val listings = source.search(config)

        assertEquals(listOf("201", "203", "204"), requested.map { it.parameters["body"] })
        val first = requested.first()
        assertEquals("/lst-caravan", first.encodedPath)
        assertEquals("U", first.parameters["ustate"])
        assertEquals("I", first.parameters["cy"])
        assertEquals("15000", first.parameters["pricefrom"])
        assertEquals("35000", first.parameters["priceto"])
        assertEquals("2008", first.parameters["fregfrom"])
        assertEquals("150000", first.parameters["kmto"])
        assertEquals(3, listings.size)
        assertEquals("mansardato", listings.first().camperType)
    }

    @Test
    fun searchesEveryTypeWhenNoTypeIsSet() = runTest {
        val requested = mutableListOf<Url>()
        val source = AutoScout24Source(client { url -> requested += url; respond(AUTO_SCOUT_24_PAGE) })

        source.search(config.copy(camperTypes = emptyList()))

        assertEquals(listOf<String?>(null), requested.map { it.parameters["body"] })
    }

    @Test
    fun pausesBetweenRequests() = runTest {
        val source = AutoScout24Source(client { respond(AUTO_SCOUT_24_PAGE) }, pauseBetweenRequests = 2.seconds)

        source.search(config)

        assertEquals(4_000, currentTime)
    }

    @Test
    fun blockedRequestIsNotAConnectionError() = runTest {
        val source = AutoScout24Source(client { respondError(HttpStatusCode.Forbidden) })

        val error = assertFailsWith<Exception> { source.search(config) }

        assertFalse(error is IOException)
    }

    private fun client(handler: suspend MockRequestHandleScope.(Url) -> HttpResponseData) =
        HttpClient(MockEngine { request -> handler(request.url) }) { expectSuccess = true }
}
