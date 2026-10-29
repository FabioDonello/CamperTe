package camperte

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ConfigTest {

    @Test
    fun readsBundledConfig() {
        val config = readConfig()

        assertEquals(15000, config.minPrice)
        assertEquals(35000, config.maxPrice)
        assertEquals(2008, config.minYear)
        assertEquals(150000, config.maxMileageKm)
        assertEquals(listOf("mansardato", "semintegrale", "furgonato"), config.camperTypes)
        assertEquals(4, config.minBerths)
        assertEquals(listOf("Lombardia", "Emilia-Romagna", "Veneto"), config.regions)
        assertTrue(config.requiredWords.isEmpty())
        assertEquals(listOf("incidentato", "da riparare", "noleggio", "cerco", "caravan", "roulotte"), config.excludedWords)
        assertEquals(listOf("letto fisso", "garage", "pannello solare", "tagliandato"), config.bonusWords)
    }

    @Test
    fun rejectsConfigWithMissingField() {
        assertFailsWith<IllegalArgumentException> {
            readConfig("""{ "min_price": 15000 }""")
        }
    }
}
