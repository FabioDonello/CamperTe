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

class SubitoSourceTest {

    private val config = readConfig()

    @Test
    fun mapsListingFromPageData() {
        val listing = parseSubitoPage(SUBITO_PAGE).first()

        assertEquals("subito-632577382", listing.id)
        assertEquals("Subito", listing.source)
        assertEquals("Camper Arca", listing.title)
        assertEquals(32000, listing.price)
        assertEquals(2009, listing.year)
        assertEquals(130000, listing.mileageKm)
        assertEquals("mansardato", listing.camperType)
        assertNull(listing.berths)
        assertEquals("Olgiate Olona (VA)", listing.location)
        assertEquals("Lombardia", listing.region)
        assertEquals(
            "https://images.sbito.it/api/v1/sbt-ads-images-pro/images/cc/cc17e691-0468-4120-bee3-6ac45b3a01bd?rule=gallery-desktop-2x-auto",
            listing.photoUrl,
        )
        assertEquals("https://www.subito.it/caravan-e-camper/camper-arca-varese-632577382.htm", listing.url)
    }

    @Test
    fun leavesMissingDataEmpty() {
        val caravan = parseSubitoPage(SUBITO_PAGE)[1]

        assertNull(caravan.year)
        assertNull(caravan.mileageKm)
        assertEquals("caravan", caravan.camperType)
    }

    @Test
    fun skipsAdvertisingSlots() {
        assertEquals(3, parseSubitoPage(SUBITO_PAGE).size)
    }

    @Test
    fun failsWhenPageHasNoListingsData() {
        assertFailsWith<IllegalStateException> { parseSubitoPage("<html><body>Accesso negato</body></html>") }
    }

    @Test
    fun requestsOnePagePerRegionWithPriceRangeAndRemovesDuplicates() = runTest {
        val requested = mutableListOf<Url>()
        val source = SubitoSource(client { url -> requested += url; respond(SUBITO_PAGE) })

        val listings = source.search(config)

        assertEquals(
            listOf(
                "https://www.subito.it/annunci-lombardia/vendita/caravan-e-camper/?ps=15000&pe=35000",
                "https://www.subito.it/annunci-emilia-romagna/vendita/caravan-e-camper/?ps=15000&pe=35000",
                "https://www.subito.it/annunci-veneto/vendita/caravan-e-camper/?ps=15000&pe=35000",
            ),
            requested.map { it.toString() },
        )
        assertEquals(listOf("subito-632577382", "subito-646487660"), listings.map { it.id })
    }

    @Test
    fun buildsSlugsForRegionsWithApostrophesAndSpaces() = runTest {
        val requested = mutableListOf<Url>()
        val source = SubitoSource(client { url -> requested += url; respond(SUBITO_PAGE) })

        source.search(config.copy(regions = listOf("Valle d'Aosta", "Trentino-Alto Adige")))

        assertEquals(
            listOf("/annunci-valle-d-aosta/vendita/caravan-e-camper/", "/annunci-trentino-alto-adige/vendita/caravan-e-camper/"),
            requested.map { it.encodedPath },
        )
    }

    @Test
    fun searchesWholeItalyWhenNoRegionIsSet() = runTest {
        val requested = mutableListOf<Url>()
        val source = SubitoSource(client { url -> requested += url; respond(SUBITO_PAGE) })

        source.search(config.copy(regions = emptyList()))

        assertEquals(listOf("/annunci-italia/vendita/caravan-e-camper/"), requested.map { it.encodedPath })
    }

    @Test
    fun pausesBetweenRequests() = runTest {
        val source = SubitoSource(client { respond(SUBITO_PAGE) }, pauseBetweenRequests = 2.seconds)

        source.search(config)

        assertEquals(4_000, currentTime)
    }

    @Test
    fun blockedRequestIsNotAConnectionError() = runTest {
        val source = SubitoSource(client { respondError(HttpStatusCode.Forbidden) })

        val error = assertFailsWith<Exception> { source.search(config) }

        assertFalse(error is IOException)
    }

    private fun client(handler: suspend MockRequestHandleScope.(Url) -> HttpResponseData) =
        HttpClient(MockEngine { request -> handler(request.url) }) { expectSuccess = true }
}
