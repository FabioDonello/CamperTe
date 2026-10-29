package camperte.sources

import camperte.Config
import camperte.Listing
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class AutoScout24Source(
    private val client: HttpClient,
    private val pauseBetweenRequests: Duration = 2.seconds,
) : Source {
    override val name = "AutoScout24"

    override suspend fun search(config: Config): List<Listing> {
        val listings = mutableListOf<Listing>()
        searchedTypes(config).forEachIndexed { index, type ->
            if (index > 0) delay(pauseBetweenRequests)
            listings += parseAutoScout24Page(fetchPage(type, config), type)
        }
        return listings.distinctBy { it.id }
    }

    private suspend fun fetchPage(type: String?, config: Config): String =
        client.get("https://www.autoscout24.it/lst-caravan") {
            parameter("atype", "N")
            parameter("cy", "I")
            parameter("ustate", "U")
            parameter("sort", "age")
            parameter("desc", "1")
            parameter("pricefrom", config.minPrice)
            parameter("priceto", config.maxPrice)
            parameter("fregfrom", config.minYear)
            parameter("kmto", config.maxMileageKm)
            type?.let { parameter("body", bodyCodes.getValue(it)) }
        }.bodyAsText()

    private fun searchedTypes(config: Config): List<String?> =
        config.camperTypes.filter { it in bodyCodes }.distinct().ifEmpty { listOf(null) }
}

internal fun parseAutoScout24Page(html: String, camperType: String?): List<Listing> {
    val page = autoScout24Json.decodeFromString<ListPage>(nextDataJson(html, "AutoScout24"))
    return page.props.pageProps.listings
        .filter { it.location?.countryCode == ITALY }
        .map { it.toListing(camperType) }
}

private const val SITE = "https://www.autoscout24.it"
private const val ITALY = "IT"
private const val PHOTO_SIZE = "800x600.webp"

private val autoScout24Json = Json { ignoreUnknownKeys = true }

private val bodyCodes = mapOf(
    "mansardato" to "201",
    "motorhome" to "202",
    "semintegrale" to "203",
    "furgonato" to "204",
)

private fun AutoScout24Item.toListing(camperType: String?): Listing = Listing(
    id = "autoscout24-$id",
    source = "AutoScout24",
    title = listOfNotNull(vehicle.make, vehicle.modelVersionInput ?: title).joinToString(" ").normalizeSpaces(),
    price = price?.priceRaw,
    year = tracking?.firstRegistration?.substringAfterLast('-')?.toIntOrNull(),
    mileageKm = tracking?.mileage?.toIntOrNull(),
    camperType = camperType,
    berths = vehicleDetails.firstOrNull { it.iconName == "bed" }?.data?.filter(Char::isDigit)?.toIntOrNull(),
    location = location?.city?.let(::placeWithProvince),
    region = location?.zip?.let(::regionFromPostcode),
    shortDescription = null,
    photoUrls = images.map { it.substringBeforeLast('/') + "/" + PHOTO_SIZE },
    url = SITE + url,
    position = null,
)

private fun placeWithProvince(city: String): String? {
    val parts = city.split('-').map { it.normalizeSpaces() }.filter { it.isNotEmpty() }
    val place = parts.firstOrNull() ?: return null
    val province = parts.drop(1).lastOrNull()?.takeIf { it.length == 2 }?.uppercase()
    return listOfNotNull(place, province?.let { "($it)" }).joinToString(" ")
}

private fun regionFromPostcode(zip: String): String? {
    val prefix = zip.trim().take(2).toIntOrNull() ?: return null
    return postcodeRegions.firstOrNull { (range, _) -> prefix in range }?.second
}

private val postcodeRegions = listOf(
    0..4 to "Lazio",
    5..6 to "Umbria",
    7..9 to "Sardegna",
    10..10 to "Piemonte",
    11..11 to "Valle d'Aosta",
    12..15 to "Piemonte",
    16..19 to "Liguria",
    20..27 to "Lombardia",
    28..28 to "Piemonte",
    29..29 to "Emilia-Romagna",
    30..32 to "Veneto",
    33..34 to "Friuli-Venezia Giulia",
    35..37 to "Veneto",
    38..39 to "Trentino-Alto Adige",
    40..44 to "Emilia-Romagna",
    45..45 to "Veneto",
    46..46 to "Lombardia",
    47..48 to "Emilia-Romagna",
    50..59 to "Toscana",
    60..63 to "Marche",
    64..67 to "Abruzzo",
    70..74 to "Puglia",
    75..75 to "Basilicata",
    76..76 to "Puglia",
    80..84 to "Campania",
    85..85 to "Basilicata",
    86..86 to "Molise",
    87..89 to "Calabria",
    90..98 to "Sicilia",
)

@Serializable
private data class ListPage(val props: ListPageProps)

@Serializable
private data class ListPageProps(val pageProps: ListPageData)

@Serializable
private data class ListPageData(val listings: List<AutoScout24Item> = emptyList())

@Serializable
private data class AutoScout24Item(
    val id: String,
    val url: String,
    val title: String? = null,
    val images: List<String> = emptyList(),
    val price: Price? = null,
    val vehicle: Vehicle = Vehicle(),
    val location: Location? = null,
    val tracking: Tracking? = null,
    val vehicleDetails: List<VehicleDetail> = emptyList(),
)

@Serializable
private data class Price(val priceRaw: Int? = null)

@Serializable
private data class Vehicle(val make: String? = null, val modelVersionInput: String? = null)

@Serializable
private data class Location(val countryCode: String? = null, val zip: String? = null, val city: String? = null)

@Serializable
private data class Tracking(val firstRegistration: String? = null, val mileage: String? = null)

@Serializable
private data class VehicleDetail(val iconName: String? = null, val data: String? = null)
