package camperte.sources

import camperte.Config
import camperte.Listing
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class SubitoSource(
    private val client: HttpClient,
    private val pauseBetweenRequests: Duration = 2.seconds,
) : Source {
    override val name = "Subito"

    override suspend fun search(config: Config): List<Listing> {
        val listings = mutableListOf<Listing>()
        regionSlugs(config).forEachIndexed { index, slug ->
            if (index > 0) delay(pauseBetweenRequests)
            listings += parseSubitoPage(fetchPage(slug, config))
        }
        return listings.distinctBy { it.id }
    }

    private suspend fun fetchPage(regionSlug: String, config: Config): String =
        client.get("https://www.subito.it/annunci-$regionSlug/vendita/caravan-e-camper/") {
            parameter("ps", config.minPrice)
            parameter("pe", config.maxPrice)
        }.bodyAsText()

    private fun regionSlugs(config: Config): List<String> =
        config.regions.map { Regex("[^a-z]+").replace(it.lowercase(), "-").trim('-') }.ifEmpty { listOf("italia") }
}

internal fun parseSubitoPage(html: String): List<Listing> {
    val start = html.indexOf(NEXT_DATA_TAG)
    check(start >= 0) { "Subito page has no listings data" }
    val jsonStart = html.indexOf('>', start) + 1
    val jsonEnd = html.indexOf("</script>", jsonStart)
    check(jsonStart > 0 && jsonEnd > jsonStart) { "Subito listings data is not closed" }
    val page = subitoJson.decodeFromString<NextData>(html.substring(jsonStart, jsonEnd))
    return page.props.pageProps.initialState.items.originalList
        .filter { it.kind == "AdItem" }
        .mapNotNull { it.toListing() }
}

private const val NEXT_DATA_TAG = "<script id=\"__NEXT_DATA__\""
private const val PHOTO_RULE = "?rule=gallery-desktop-2x-auto"

private val subitoJson = Json { ignoreUnknownKeys = true }

private val camperTypes = mapOf(
    "mansardato" to "mansardato",
    "semi-integrale" to "semintegrale",
    "motorhome" to "motorhome",
    "camper puro" to "furgonato",
    "caravan/roulotte" to "caravan",
)

private fun SubitoItem.toListing(): Listing? {
    val url = urls?.default ?: return null
    val title = subject ?: return null
    return Listing(
        id = "subito-${urn.substringAfterLast(':')}",
        source = "Subito",
        title = title,
        price = feature("/price")?.key?.toIntOrNull(),
        year = feature("/year")?.key?.toIntOrNull(),
        mileageKm = feature("/mileage_scalar")?.key?.toIntOrNull(),
        camperType = feature("/caravan_type")?.value?.lowercase()?.let(camperTypes::get),
        berths = null,
        location = geo?.location(),
        region = geo?.region?.value,
        shortDescription = body,
        photoUrl = images.firstOrNull()?.cdnBaseUrl?.let { it + PHOTO_RULE },
        url = url,
    )
}

private fun SubitoItem.feature(uri: String): FeatureValue? = features[uri]?.values?.firstOrNull()

private fun Geo.location(): String? {
    val townName = town?.value ?: return null
    return listOfNotNull(townName, city?.shortName?.let { "($it)" }).joinToString(" ")
}

@Serializable
private data class NextData(val props: Props)

@Serializable
private data class Props(val pageProps: PageProps)

@Serializable
private data class PageProps(val initialState: InitialState)

@Serializable
private data class InitialState(val items: Items)

@Serializable
private data class Items(val originalList: List<SubitoItem> = emptyList())

@Serializable
private data class SubitoItem(
    val kind: String,
    val urn: String = "",
    val subject: String? = null,
    val body: String? = null,
    val images: List<Image> = emptyList(),
    val features: Map<String, Feature> = emptyMap(),
    val geo: Geo? = null,
    val urls: Urls? = null,
)

@Serializable
private data class Image(val cdnBaseUrl: String? = null)

@Serializable
private data class Feature(val values: List<FeatureValue> = emptyList())

@Serializable
private data class FeatureValue(val key: String? = null, val value: String? = null)

@Serializable
private data class Geo(val region: Named? = null, val city: City? = null, val town: Named? = null)

@Serializable
private data class Named(val value: String? = null)

@Serializable
private data class City(val shortName: String? = null)

@Serializable
private data class Urls(@SerialName("default") val default: String? = null)
