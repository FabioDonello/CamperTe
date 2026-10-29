package camperte.sources

import camperte.Config
import camperte.Listing
import camperte.italianRegionName
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parameters
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class CamperOnLineSource(
    private val client: HttpClient,
    private val pauseBetweenRequests: Duration = 2.seconds,
) : Source {
    override val name = "CamperOnLine"

    override suspend fun search(config: Config): List<Listing> {
        val listings = mutableListOf<Listing>()
        searchedTypeCodes(config).forEachIndexed { index, typeCode ->
            if (index > 0) delay(pauseBetweenRequests)
            listings += parseCamperOnLinePage(fetchPage(typeCode, config))
        }
        return listings.distinctBy { it.id }
    }

    private suspend fun fetchPage(typeCode: String, config: Config): String =
        client.submitForm(
            url = "https://www.camperonline.it/camper-usati",
            formParameters = parameters {
                append("tp", typeCode)
                append("pmin", config.minPrice.toString())
                append("pmax", config.maxPrice.toString())
                append("an_min", config.minYear.toString())
                append("isPrivato", "2")
                append("sottosezione", "U")
                append("offerta_list[]", "1,4,5,6")
            },
        ).bodyAsText()

    private fun searchedTypeCodes(config: Config): List<String> =
        config.camperTypes.mapNotNull(typeCodes::get).distinct().ifEmpty { listOf(ANY_TYPE) }
}

internal fun parseCamperOnLinePage(html: String): List<Listing> {
    check(html.contains(RESULTS_MARKER)) { "CamperOnLine page has no results section" }
    return html.split(LISTING_MARKER).drop(1).mapNotNull(::parseListing)
}

private const val RESULTS_MARKER = "<div id=\"resultInclude\">"
private const val LISTING_MARKER = "<div class=\"row inserzione "
private const val ANY_TYPE = "0"
private const val PHOTO_BASE = "https://thumb.camperonline.it/timthumb.php?src="
private const val PHOTO_SIZE = "&w=1000&h=650"

private val camperTypes = mapOf(
    "Mansardato" to "mansardato",
    "Semintegrale" to "semintegrale",
    "Motorhome" to "motorhome",
    "Van, Furgonato" to "furgonato",
    "Caravan" to "caravan",
)

private val typeCodes = mapOf(
    "mansardato" to "1",
    "semintegrale" to "2",
    "motorhome" to "3",
    "furgonato" to "4",
)

private val titleLink = Regex("""<a href="([^"]+)" class="titolo-elemento-lista" title="([^"]*)"""")
private val listingNumber = Regex("""_(\d+)$""")
private val price = Regex("""inserzione_tipo_mobile"><div>([^<]*)<""")
private val photo = Regex("""<img [^>]*src="[^"]*timthumb\.php\?src=([^"&]+)""")
private val town = Regex("""fa-city"></i>([^<]*?)\s*-\s*<i>""")
private val province = Regex("""\s*(\([A-Z]{2}\))""")
private val whitespace = Regex("""\s+""")

private fun parseListing(block: String): Listing? {
    val link = titleLink.find(block) ?: return null
    val url = link.groupValues[1]
    val number = listingNumber.find(url)?.groupValues?.get(1) ?: return null
    val fullTitle = decodeEntities(link.groupValues[2]).normalizeSpaces()
    val typeLabel = camperTypes.keys.firstOrNull { fullTitle.startsWith("$it ") }
    return Listing(
        id = "camperonline-$number",
        source = "CamperOnLine",
        title = typeLabel?.let { fullTitle.removePrefix(it).trim() } ?: fullTitle,
        price = price.find(block)?.groupValues?.get(1)?.toNumber(),
        year = block.field("Anno")?.toNumber(),
        mileageKm = block.field("Km")?.toNumber(),
        camperType = typeLabel?.let(camperTypes::get),
        berths = block.field("Posti/Letti")?.substringAfter('/')?.toNumber(),
        location = town.find(block)?.groupValues?.get(1)?.let(::location),
        region = block.field("Regione")?.let(::italianRegionName),
        shortDescription = null,
        photoUrls = listOfNotNull(photo.find(block)?.groupValues?.get(1)?.let { "$PHOTO_BASE$it$PHOTO_SIZE" }),
        url = url,
        position = null,
    )
}

private fun String.field(label: String): String? =
    Regex("""</i> ${Regex.escape(label)}</span><h6>([^<]*)</h6>""").find(this)?.groupValues?.get(1)?.trim()?.ifEmpty { null }

private fun String.toNumber(): Int? = filter(Char::isDigit).toIntOrNull()

private fun location(text: String): String? {
    val provinces = province.findAll(text).map { it.groupValues[1] }.distinct().toList()
    val place = decodeEntities(province.replace(text, "")).normalizeSpaces()
    return (listOf(place) + provinces).filter { it.isNotEmpty() }.joinToString(" ").ifEmpty { null }
}

private fun String.normalizeSpaces(): String = whitespace.replace(this, " ").trim()

private fun decodeEntities(text: String): String =
    Regex("""&(#\d+|amp|quot|apos|lt|gt);""").replace(text) { match ->
        when (val entity = match.groupValues[1]) {
            "amp" -> "&"
            "quot" -> "\""
            "apos" -> "'"
            "lt" -> "<"
            "gt" -> ">"
            else -> entity.drop(1).toIntOrNull()?.toChar()?.toString() ?: match.value
        }
    }
