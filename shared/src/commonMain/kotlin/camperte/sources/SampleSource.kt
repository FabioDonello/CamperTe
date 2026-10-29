package camperte.sources

import camperte.Config
import camperte.Listing

internal class SampleSource : Source {
    override val name = "Esempio"

    override suspend fun search(config: Config): List<Listing> = listOf(
        sample(
            number = 1,
            title = "Mansardato Laika Ecovip con letto fisso e garage",
            price = 28500,
            year = 2012,
            mileageKm = 98000,
            camperType = "mansardato",
            berths = 6,
            location = "Brescia",
            region = "Lombardia",
            description = "Tagliandato, pannello solare, gomme nuove.",
        ),
        sample(
            number = 2,
            title = "Semintegrale Rimor Seal 5",
            price = 33900,
            year = 2015,
            mileageKm = 72000,
            camperType = "semintegrale",
            berths = 4,
            location = "Modena",
            region = "Emilia-Romagna",
            description = "Unico proprietario, sempre in rimessaggio.",
        ),
        sample(
            number = 3,
            title = "Camper Elnagh Baron",
            price = 19800,
            year = null,
            mileageKm = null,
            camperType = null,
            berths = null,
            location = "Verona",
            region = "Veneto",
            description = null,
        ),
        sample(
            number = 4,
            title = "Mansardato incidentato, da riparare",
            price = 9000,
            year = 2009,
            mileageKm = 160000,
            camperType = "mansardato",
            berths = 6,
            location = "Padova",
            region = "Veneto",
            description = "Vendo per ricambi.",
        ),
    )

    private fun sample(
        number: Int,
        title: String,
        price: Int?,
        year: Int?,
        mileageKm: Int?,
        camperType: String?,
        berths: Int?,
        location: String,
        region: String,
        description: String?,
    ) = Listing(
        id = "sample-$number",
        source = name,
        title = title,
        price = price,
        year = year,
        mileageKm = mileageKm,
        camperType = camperType,
        berths = berths,
        location = location,
        region = region,
        shortDescription = description,
        photoUrl = if (number % 2 == 1) "https://picsum.photos/seed/camperte-$number/800/500" else null,
        url = "https://example.com/camperte/$number",
    )
}
