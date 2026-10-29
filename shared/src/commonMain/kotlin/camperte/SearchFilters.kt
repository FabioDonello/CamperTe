package camperte

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface SearchArea {
    @Serializable
    @SerialName("regions")
    data object ConfiguredRegions : SearchArea

    @Serializable
    @SerialName("nearby")
    data class Nearby(val km: Int) : SearchArea

    @Serializable
    @SerialName("italy")
    data object WholeItaly : SearchArea
}

@Serializable
data class SearchFilters(
    val minPrice: Int,
    val maxPrice: Int,
    val minYear: Int,
    val maxMileageKm: Int,
    val camperTypes: List<String>,
    val area: SearchArea,
) {
    val minPriceText: String get() = "€ ${minPrice.withThousandsSeparator()}"

    val maxPriceText: String get() = "€ ${maxPrice.withThousandsSeparator()}"

    val maxMileageText: String get() = "${maxMileageKm.withThousandsSeparator()} km"

    fun stepMinPrice(up: Boolean): SearchFilters =
        copy(minPrice = (minPrice + step(up, PRICE_STEP)).coerceIn(0, maxPrice))

    fun stepMaxPrice(up: Boolean): SearchFilters =
        copy(maxPrice = (maxPrice + step(up, PRICE_STEP)).coerceAtLeast(minPrice))

    fun stepMinYear(up: Boolean): SearchFilters =
        copy(minYear = (minYear + step(up, 1)).coerceIn(MIN_YEAR, MAX_YEAR))

    fun stepMaxMileage(up: Boolean): SearchFilters =
        copy(maxMileageKm = (maxMileageKm + step(up, MILEAGE_STEP)).coerceAtLeast(MILEAGE_STEP))

    fun toggleCamperType(type: String): SearchFilters =
        copy(camperTypes = if (type in camperTypes) camperTypes - type else camperTypes + type)

    fun withArea(area: SearchArea): SearchFilters = copy(area = area)

    fun summaryText(configuredRegions: List<String>, hasPosition: Boolean): String = listOf(
        "$minPriceText – ${maxPrice.withThousandsSeparator()}",
        areaText(configuredRegions, hasPosition),
        "dal $minYear",
        "fino a $maxMileageText",
        camperTypes.ifEmpty { listOf("tutte le tipologie") }.joinToString(", "),
    ).joinToString(" · ")

    private fun areaText(configuredRegions: List<String>, hasPosition: Boolean): String = when (area) {
        SearchArea.ConfiguredRegions -> configuredRegions.joinToString(", ")
        is SearchArea.Nearby -> if (hasPosition) "entro ${area.km} km" else configuredRegions.joinToString(", ")
        SearchArea.WholeItaly -> "tutta Italia"
    }

    companion object {
        val camperTypeOptions = listOf("mansardato", "semintegrale", "furgonato", "motorhome")
        val distanceOptionsKm = listOf(50, 100, 150, 300)

        fun from(config: Config) = SearchFilters(
            minPrice = config.minPrice,
            maxPrice = config.maxPrice,
            minYear = config.minYear,
            maxMileageKm = config.maxMileageKm,
            camperTypes = config.camperTypes,
            area = SearchArea.ConfiguredRegions,
        )
    }
}

private const val PRICE_STEP = 1_000
private const val MILEAGE_STEP = 10_000
private const val MIN_YEAR = 1980
private const val MAX_YEAR = 2100

private fun step(up: Boolean, amount: Int): Int = if (up) amount else -amount
