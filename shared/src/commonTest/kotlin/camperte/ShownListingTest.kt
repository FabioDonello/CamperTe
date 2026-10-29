package camperte

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShownListingTest {

    @Test
    fun formatsPriceInItalianStyle() {
        assertEquals("€ 28.500", shown(price = 28500).priceText)
        assertEquals("€ 950", shown(price = 950).priceText)
        assertEquals("€ 1.250.000", shown(price = 1_250_000).priceText)
    }

    @Test
    fun hasNoPriceTextWhenPriceIsMissing() {
        assertNull(shown(price = null).priceText)
    }

    @Test
    fun joinsAvailableDetails() {
        val complete = shown(year = 2012, mileageKm = 98000, camperType = "mansardato", berths = 6, location = "Brescia")

        assertEquals("2012 · 98.000 km · mansardato · 6 posti letto · Brescia", complete.detailsText)
    }

    @Test
    fun omitsMissingDetails() {
        assertEquals("Verona", shown(location = "Verona").detailsText)
        assertEquals("", shown().detailsText)
    }

    @Test
    fun usesSingularForOneBerth() {
        assertEquals("1 posto letto", shown(berths = 1).detailsText)
    }

    private fun shown(
        price: Int? = null,
        year: Int? = null,
        mileageKm: Int? = null,
        camperType: String? = null,
        berths: Int? = null,
        location: String? = null,
    ) = ShownListing(
        listing = Listing(
            id = "test-1",
            source = "Test",
            title = "Camper",
            price = price,
            year = year,
            mileageKm = mileageKm,
            camperType = camperType,
            berths = berths,
            location = location,
            region = null,
            shortDescription = null,
            photoUrls = emptyList(),
            url = "https://example.com/test-1",
        ),
        bonusWords = emptyList(),
        isNew = false,
    )
}
