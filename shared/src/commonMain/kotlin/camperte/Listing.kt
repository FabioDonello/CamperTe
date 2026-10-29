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
)
