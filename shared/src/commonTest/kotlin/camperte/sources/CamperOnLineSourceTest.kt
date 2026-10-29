package camperte.sources

import camperte.readConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.parseQueryString
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

class CamperOnLineSourceTest {

    private val config = readConfig()

    @Test
    fun mapsListingFromPage() {
        val listing = parseCamperOnLinePage(CAMPER_ON_LINE_PAGE).first()

        assertEquals("camperonline-201551", listing.id)
        assertEquals("CamperOnLine", listing.source)
        assertEquals("C.i. Elliot Garage Modello 2012", listing.title)
        assertEquals(32400, listing.price)
        assertEquals(2011, listing.year)
        assertEquals(83000, listing.mileageKm)
        assertEquals("mansardato", listing.camperType)
        assertEquals(6, listing.berths)
        assertEquals("Cavarzere (VE)", listing.location)
        assertEquals("Veneto", listing.region)
        assertEquals(
            listOf(
                "https://thumb.camperonline.it/timthumb.php?src=" +
                    "https://www.camperonline.it/camper-nuovi-usati/Foto/1793271390_201551_0_1okimg20261024142333.jpg&w=1000&h=650",
            ),
            listing.photoUrls,
        )
        assertEquals("https://www.camperonline.it/camper-usati/mansardato-c-i-elliot-garage-modello-2012_201551", listing.url)
        assertNull(listing.position)
    }

    @Test
    fun readsVanTypeAndBedsRatherThanSeats() {
        val van = parseCamperOnLinePage(CAMPER_ON_LINE_PAGE)[1]

        assertEquals("furgonato", van.camperType)
        assertEquals("Laika Ecovip 540", van.title)
        assertEquals(2, van.berths)
        assertNull(van.mileageKm)
    }

    @Test
    fun leavesMissingDataEmptyAndUsesAppRegionNames() {
        val semi = parseCamperOnLinePage(CAMPER_ON_LINE_PAGE)[2]

        assertNull(semi.berths)
        assertEquals("Emilia-Romagna", semi.region)
        assertEquals("Vignola (MO)", semi.location)
    }

    @Test
    fun recognisesCaravans() {
        val caravan = parseCamperOnLinePage(CAMPER_ON_LINE_PAGE)[3]

        assertEquals("caravan", caravan.camperType)
        assertEquals("Novi Ligure (AL)", caravan.location)
    }

    @Test
    fun readsPriceOnRequestAsMissing() {
        val page = CAMPER_ON_LINE_PAGE.replace("<div>€ 32.400</div>", "<div>Trattativa riservata</div>")

        assertNull(parseCamperOnLinePage(page).first().price)
    }

    @Test
    fun failsWhenPageHasNoResultsSection() {
        assertFailsWith<IllegalStateException> { parseCamperOnLinePage("<html><body>Errore</body></html>") }
    }

    @Test
    fun postsOneSearchPerCamperTypeWithPriceAndYear() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val source = CamperOnLineSource(client { request -> requests += request; respond(CAMPER_ON_LINE_PAGE) })

        val listings = source.search(config)

        assertEquals(listOf(HttpMethod.Post), requests.map { it.method }.distinct())
        assertEquals("https://www.camperonline.it/camper-usati", requests.first().url.toString())
        val forms = requests.map { parseQueryString((it.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()) }
        assertEquals(listOf("1", "2", "4"), forms.map { it["tp"] })
        assertEquals("15000", forms.first()["pmin"])
        assertEquals("35000", forms.first()["pmax"])
        assertEquals("2008", forms.first()["an_min"])
        assertEquals(4, listings.size)
    }

    @Test
    fun searchesEveryTypeWhenNoTypeIsSet() = runTest {
        val types = mutableListOf<String?>()
        val source = CamperOnLineSource(client { request ->
            types += parseQueryString((request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString())["tp"]
            respond(CAMPER_ON_LINE_PAGE)
        })

        source.search(config.copy(camperTypes = emptyList()))

        assertEquals(listOf<String?>("0"), types)
    }

    @Test
    fun pausesBetweenRequests() = runTest {
        val source = CamperOnLineSource(client { respond(CAMPER_ON_LINE_PAGE) }, pauseBetweenRequests = 2.seconds)

        source.search(config)

        assertEquals(4_000, currentTime)
    }

    @Test
    fun blockedRequestIsNotAConnectionError() = runTest {
        val source = CamperOnLineSource(client { respondError(HttpStatusCode.Forbidden) })

        val error = assertFailsWith<Exception> { source.search(config) }

        assertFalse(error is IOException)
    }

    private fun client(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        HttpClient(MockEngine { request -> handler(request) }) { expectSuccess = true }
}
