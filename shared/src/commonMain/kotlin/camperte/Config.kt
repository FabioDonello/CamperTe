package camperte

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val CONFIG_JSON = """
{
  "prezzo_min": 15000,
  "prezzo_max": 35000,
  "anno_min": 2008,
  "km_max": 150000,
  "tipologie": ["mansardato", "semintegrale"],
  "posti_letto_min": 4,
  "regioni": ["Lombardia", "Emilia-Romagna", "Veneto"],
  "parole_obbligatorie": [],
  "parole_escluse": ["incidentato", "da riparare", "noleggio", "cerco"],
  "parole_bonus": ["letto fisso", "garage", "pannello solare", "tagliandato"]
}
"""

@Serializable
data class Config(
    @SerialName("prezzo_min") val prezzoMin: Int,
    @SerialName("prezzo_max") val prezzoMax: Int,
    @SerialName("anno_min") val annoMin: Int,
    @SerialName("km_max") val kmMax: Int,
    val tipologie: List<String>,
    @SerialName("posti_letto_min") val postiLettoMin: Int,
    val regioni: List<String>,
    @SerialName("parole_obbligatorie") val paroleObbligatorie: List<String>,
    @SerialName("parole_escluse") val paroleEscluse: List<String>,
    @SerialName("parole_bonus") val paroleBonus: List<String>,
)

fun leggiConfig(json: String = CONFIG_JSON): Config = Json.decodeFromString(json)
