package camperte

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FilterTest {

    private val config = readConfig()

    @Test
    fun keepsListingWithAllDataMissing() {
        assertTrue(matchesCriteria(listing(), config))
    }

    @Test
    fun keepsListingMatchingEveryCriterion() {
        val complete = listing(
            price = 28500,
            year = 2012,
            mileageKm = 120000,
            camperType = "Mansardato",
            berths = 6,
            region = "lombardia",
            description = "Tenuto benissimo",
        )

        assertTrue(matchesCriteria(complete, config))
    }

    @Test
    fun discardsPriceOutsideRange() {
        assertFalse(matchesCriteria(listing(price = 14999), config))
        assertFalse(matchesCriteria(listing(price = 35001), config))
        assertTrue(matchesCriteria(listing(price = 15000), config))
        assertTrue(matchesCriteria(listing(price = 35000), config))
    }

    @Test
    fun discardsTooOldYear() {
        assertFalse(matchesCriteria(listing(year = 2007), config))
        assertTrue(matchesCriteria(listing(year = 2008), config))
    }

    @Test
    fun discardsTooHighMileage() {
        assertFalse(matchesCriteria(listing(mileageKm = 150001), config))
        assertTrue(matchesCriteria(listing(mileageKm = 150000), config))
    }

    @Test
    fun discardsTooFewBerths() {
        assertFalse(matchesCriteria(listing(berths = 3), config))
        assertTrue(matchesCriteria(listing(berths = 4), config))
    }

    @Test
    fun discardsOtherKnownCamperType() {
        assertFalse(matchesCriteria(listing(camperType = "integrale"), config))
        assertTrue(matchesCriteria(listing(camperType = " Semintegrale "), config))
    }

    @Test
    fun keepsVansAndDiscardsCaravans() {
        assertTrue(matchesCriteria(listing(camperType = "furgonato"), config))
        assertFalse(matchesCriteria(listing(title = "Roulotte Burstner Averso"), config))
        assertFalse(matchesCriteria(listing(title = "Knaus Sport", description = "Caravan 4 posti"), config))
    }

    @Test
    fun acceptsAnyCamperTypeWhenListIsEmpty() {
        assertTrue(matchesCriteria(listing(camperType = "integrale"), config.copy(camperTypes = emptyList())))
    }

    @Test
    fun discardsRegionOutsideList() {
        assertFalse(matchesCriteria(listing(region = "Sicilia"), config))
        assertTrue(matchesCriteria(listing(region = "Emilia-Romagna"), config))
    }

    @Test
    fun acceptsAnyRegionWhenListIsEmpty() {
        assertTrue(matchesCriteria(listing(region = "Sicilia"), config.copy(regions = emptyList())))
    }

    @Test
    fun discardsExcludedWordInTitleOrDescriptionIgnoringCase() {
        assertFalse(matchesCriteria(listing(title = "CERCO camper mansardato"), config))
        assertFalse(matchesCriteria(listing(description = "Leggermente incidentato"), config))
    }

    @Test
    fun ignoresBlankExcludedWord() {
        assertTrue(matchesCriteria(listing(), config.copy(excludedWords = listOf(" "))))
    }

    @Test
    fun requiresEveryRequiredWord() {
        val withRequired = config.copy(requiredWords = listOf("doccia", "veranda"))

        assertFalse(matchesCriteria(listing(description = "Con doccia separata"), withRequired))
        assertTrue(matchesCriteria(listing(title = "Veranda", description = "Con doccia separata"), withRequired))
    }

    @Test
    fun keepsListingWithoutDescriptionWhenRequiredWordsCannotBeChecked() {
        val withRequired = config.copy(requiredWords = listOf("doccia"))

        assertTrue(matchesCriteria(listing(description = null), withRequired))
    }

    @Test
    fun findsBonusWordsInConfigOrder() {
        val withBonus = listing(title = "Mansardato con GARAGE", description = "Letto fisso, tagliandato")

        assertEquals(listOf("letto fisso", "garage", "tagliandato"), findBonusWords(withBonus, config))
    }

    @Test
    fun findsNoBonusWordsWhenAbsent() {
        assertEquals(emptyList(), findBonusWords(listing(), config))
    }

    @Test
    fun sortsNewFirstThenBonusThenPriceWithMissingPriceLast() {
        val oldExpensive = shown("old-expensive", price = 30000)
        val oldWithoutPrice = shown("old-without-price", price = null)
        val oldCheap = shown("old-cheap", price = 20000)
        val oldWithBonus = shown("old-bonus", price = 34000, bonusWords = listOf("garage"))
        val newExpensive = shown("new-expensive", price = 35000, isNew = true)

        val sorted = sortListings(listOf(oldExpensive, oldWithoutPrice, oldCheap, oldWithBonus, newExpensive))

        assertEquals(
            listOf("new-expensive", "old-bonus", "old-cheap", "old-expensive", "old-without-price"),
            sorted.map { it.listing.id },
        )
    }

    private fun listing(
        id: String = "test-1",
        title: String = "Camper",
        price: Int? = null,
        year: Int? = null,
        mileageKm: Int? = null,
        camperType: String? = null,
        berths: Int? = null,
        region: String? = null,
        description: String? = null,
    ) = Listing(
        id = id,
        source = "Test",
        title = title,
        price = price,
        year = year,
        mileageKm = mileageKm,
        camperType = camperType,
        berths = berths,
        location = null,
        region = region,
        shortDescription = description,
        photoUrl = null,
        url = "https://example.com/$id",
    )

    private fun shown(id: String, price: Int?, bonusWords: List<String> = emptyList(), isNew: Boolean = false) =
        ShownListing(listing(id = id, price = price), bonusWords, isNew)
}
