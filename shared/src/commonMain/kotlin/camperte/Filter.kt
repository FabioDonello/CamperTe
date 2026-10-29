package camperte

internal fun matchesCriteria(listing: Listing, config: Config): Boolean =
    inRange(listing.price, config.minPrice, config.maxPrice) &&
        atLeast(listing.year, config.minYear) &&
        atMost(listing.mileageKm, config.maxMileageKm) &&
        atLeast(listing.berths, config.minBerths) &&
        isAllowed(listing.camperType, config.camperTypes) &&
        isAllowed(listing.region, config.regions) &&
        containsRequiredWords(listing, config.requiredWords) &&
        wordsFound(listing, config.excludedWords).isEmpty()

internal fun findBonusWords(listing: Listing, config: Config): List<String> =
    wordsFound(listing, config.bonusWords)

internal fun sortListings(listings: List<ShownListing>): List<ShownListing> =
    listings.sortedWith(
        compareByDescending<ShownListing> { it.isNew }
            .thenByDescending { it.bonusWords.size }
            .thenBy(nullsLast()) { it.listing.price }
    )

private fun inRange(value: Int?, min: Int, max: Int): Boolean = value == null || value in min..max

private fun atLeast(value: Int?, min: Int): Boolean = value == null || value >= min

private fun atMost(value: Int?, max: Int): Boolean = value == null || value <= max

private fun isAllowed(value: String?, allowed: List<String>): Boolean =
    value == null || allowed.isEmpty() || allowed.any { it.trim().equals(value.trim(), ignoreCase = true) }

private fun containsRequiredWords(listing: Listing, words: List<String>): Boolean =
    listing.shortDescription == null || words.all { it.isBlank() || listing.searchableText().containsWord(it) }

private fun wordsFound(listing: Listing, words: List<String>): List<String> {
    val text = listing.searchableText()
    return words.filter { text.containsWord(it) }
}

private fun Listing.searchableText(): String = listOfNotNull(title, shortDescription).joinToString(" ").lowercase()

private fun String.containsWord(word: String): Boolean = word.isNotBlank() && word.trim().lowercase() in this
