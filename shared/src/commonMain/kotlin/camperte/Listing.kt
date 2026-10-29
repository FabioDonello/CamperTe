package camperte

data class Listing(
    val id: String,
    val source: String,
    val title: String,
    val price: Int?,
    val year: Int?,
    val mileageKm: Int?,
    val camperType: String?,
    val berths: Int?,
    val location: String?,
    val region: String?,
    val shortDescription: String?,
    val photoUrl: String?,
    val url: String,
)

data class ShownListing(
    val listing: Listing,
    val bonusWords: List<String>,
    val isNew: Boolean,
) {
    val priceText: String? get() = listing.price?.let { "€ ${it.withThousandsSeparator()}" }

    val detailsText: String
        get() = listOfNotNull(
            listing.year?.toString(),
            listing.mileageKm?.let { "${it.withThousandsSeparator()} km" },
            listing.camperType,
            listing.berths?.let { if (it == 1) "1 posto letto" else "$it posti letto" },
            listing.location,
        ).joinToString(" · ")
}

private fun Int.withThousandsSeparator(): String = toString().reversed().chunked(3).joinToString(".").reversed()
