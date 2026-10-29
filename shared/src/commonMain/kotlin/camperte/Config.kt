package camperte

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val CONFIG_JSON = """
{
  "min_price": 15000,
  "max_price": 35000,
  "min_year": 2008,
  "max_mileage_km": 150000,
  "camper_types": ["mansardato", "semintegrale"],
  "min_berths": 4,
  "regions": ["Lombardia", "Emilia-Romagna", "Veneto"],
  "required_words": [],
  "excluded_words": ["incidentato", "da riparare", "noleggio", "cerco"],
  "bonus_words": ["letto fisso", "garage", "pannello solare", "tagliandato"]
}
"""

@Serializable
data class Config(
    @SerialName("min_price") val minPrice: Int,
    @SerialName("max_price") val maxPrice: Int,
    @SerialName("min_year") val minYear: Int,
    @SerialName("max_mileage_km") val maxMileageKm: Int,
    @SerialName("camper_types") val camperTypes: List<String>,
    @SerialName("min_berths") val minBerths: Int,
    val regions: List<String>,
    @SerialName("required_words") val requiredWords: List<String>,
    @SerialName("excluded_words") val excludedWords: List<String>,
    @SerialName("bonus_words") val bonusWords: List<String>,
)

fun readConfig(json: String = CONFIG_JSON): Config = Json.decodeFromString(json)
