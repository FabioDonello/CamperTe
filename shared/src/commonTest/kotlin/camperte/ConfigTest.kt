package camperte

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ConfigTest {

    @Test
    fun readsBundledConfig() {
        val config = leggiConfig()

        assertEquals(15000, config.prezzoMin)
        assertEquals(35000, config.prezzoMax)
        assertEquals(2008, config.annoMin)
        assertEquals(150000, config.kmMax)
        assertEquals(listOf("mansardato", "semintegrale"), config.tipologie)
        assertEquals(4, config.postiLettoMin)
        assertEquals(listOf("Lombardia", "Emilia-Romagna", "Veneto"), config.regioni)
        assertTrue(config.paroleObbligatorie.isEmpty())
        assertEquals(listOf("incidentato", "da riparare", "noleggio", "cerco"), config.paroleEscluse)
        assertEquals(listOf("letto fisso", "garage", "pannello solare", "tagliandato"), config.paroleBonus)
    }

    @Test
    fun rejectsConfigWithMissingField() {
        assertFailsWith<IllegalArgumentException> {
            leggiConfig("""{ "prezzo_min": 15000 }""")
        }
    }
}
