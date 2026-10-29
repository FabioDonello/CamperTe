package camperte

import camperte.sources.Source
import camperte.sources.activeSources
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.io.IOException
import kotlin.math.roundToInt
import kotlin.time.Clock

class CamperRepository internal constructor(
    private val sources: List<Source>,
    private val config: Config,
    private val clock: Clock,
    private val filtersStore: FiltersStore,
) {
    constructor(sources: List<Source>, config: Config, clock: Clock, settings: Settings) :
        this(sources, config, clock, FiltersStore(settings, SearchFilters.from(config)))

    constructor() : this(activeSources(), readConfig(), Clock.System, Settings())

    val defaultFilters: SearchFilters get() = SearchFilters.from(config)

    fun currentFilters(): SearchFilters = filtersStore.load()

    fun saveFilters(filters: SearchFilters) = filtersStore.save(filters)

    fun filtersSummary(filters: SearchFilters, hasPosition: Boolean): String =
        filters.summaryText(config.regions, hasPosition)

    suspend fun loadListings(position: GeoPoint?): SearchResult {
        val filters = filtersStore.load()
        val searchConfig = searchConfig(filters, position)
        val outcomes = searchAll(searchConfig)
        val failures = outcomes.filterIsInstance<SourceOutcome.Failed>()
        if (failures.isNotEmpty() && failures.size == outcomes.size && failures.all { it.isConnectionError }) {
            return SearchResult.NoConnection
        }
        val maxDistanceKm = (filters.area as? SearchArea.Nearby)?.km
        val filterConfig = if (maxDistanceKm != null && position != null) searchConfig.copy(regions = emptyList()) else searchConfig
        val listings = outcomes.filterIsInstance<SourceOutcome.Succeeded>().flatMap { it.listings }
        return SearchResult.Found(
            listings = sortListings(
                listings
                    .filter { matchesCriteria(it, filterConfig) && isWithinDistance(it, position, maxDistanceKm) }
                    .map { toShown(it, position) }
            ),
            updatedAt = clock.now(),
            unavailableSources = failures.map { it.sourceName },
        )
    }

    private fun searchConfig(filters: SearchFilters, position: GeoPoint?): Config = config.copy(
        minPrice = filters.minPrice,
        maxPrice = filters.maxPrice,
        minYear = filters.minYear,
        maxMileageKm = filters.maxMileageKm,
        camperTypes = filters.camperTypes,
        regions = searchRegions(filters.area, position),
    )

    private fun searchRegions(area: SearchArea, position: GeoPoint?): List<String> = when (area) {
        SearchArea.ConfiguredRegions -> config.regions
        is SearchArea.Nearby -> position?.let { regionsNear(it, area.km) } ?: config.regions
        SearchArea.WholeItaly -> emptyList()
    }

    private suspend fun searchAll(searchConfig: Config): List<SourceOutcome> = supervisorScope {
        sources
            .map { source -> source to async { source.search(searchConfig) } }
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

    private fun toShown(listing: Listing, position: GeoPoint?): ShownListing {
        val listingPosition = listing.position
        val distance = if (position != null && listingPosition != null) distanceKm(position, listingPosition).roundToInt() else null
        return ShownListing(listing, findBonusWords(listing, config), isNew = false, distanceKm = distance)
    }

    private sealed interface SourceOutcome {
        data class Succeeded(val listings: List<Listing>) : SourceOutcome
        data class Failed(val sourceName: String, val isConnectionError: Boolean) : SourceOutcome
    }
}
