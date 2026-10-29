package camperte

import camperte.sources.Source
import camperte.sources.activeSources
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.io.IOException
import kotlin.time.Clock

class CamperRepository internal constructor(
    private val sources: List<Source>,
    private val config: Config,
    private val clock: Clock,
) {
    constructor() : this(activeSources, readConfig(), Clock.System)

    suspend fun loadListings(): SearchResult {
        val outcomes = searchAll()
        val failures = outcomes.filterIsInstance<SourceOutcome.Failed>()
        if (failures.isNotEmpty() && failures.size == outcomes.size && failures.all { it.isConnectionError }) {
            return SearchResult.NoConnection
        }
        val listings = outcomes.filterIsInstance<SourceOutcome.Succeeded>().flatMap { it.listings }
        return SearchResult.Found(
            listings = sortListings(listings.filter { matchesCriteria(it, config) }.map(::toShown)),
            updatedAt = clock.now(),
            unavailableSources = failures.map { it.sourceName },
        )
    }

    private suspend fun searchAll(): List<SourceOutcome> = supervisorScope {
        sources
            .map { source -> source to async { source.search(config) } }
            .map { (source, search) -> awaitOutcome(source, search) }
    }

    private suspend fun awaitOutcome(source: Source, search: Deferred<List<Listing>>): SourceOutcome =
        try {
            SourceOutcome.Succeeded(search.await())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SourceOutcome.Failed(source.name, isConnectionError = e is IOException)
        }

    private fun toShown(listing: Listing) = ShownListing(listing, findBonusWords(listing, config), isNew = false)

    private sealed interface SourceOutcome {
        data class Succeeded(val listings: List<Listing>) : SourceOutcome
        data class Failed(val sourceName: String, val isConnectionError: Boolean) : SourceOutcome
    }
}
