package camperte

import kotlin.time.Instant

sealed interface SearchResult {
    data class Found(
        val listings: List<ShownListing>,
        val updatedAt: Instant,
        val unavailableSources: List<String>,
    ) : SearchResult {
        val newCount: Int get() = listings.count { it.isNew }
    }

    data object NoConnection : SearchResult
}
