package camperte.android

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import camperte.CamperRepository
import camperte.GeoPoint
import camperte.SearchArea
import camperte.SearchFilters
import camperte.SearchResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class ListingsState(val result: SearchResult?, val isRefreshing: Boolean, val filtersSummary: String)

data class FiltersState(
    val filters: SearchFilters,
    val locationDenied: Boolean,
    val pendingDistanceKm: Int?,
)

class ListingsViewModel(
    private val repository: CamperRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    var state by mutableStateOf(ListingsState(result = null, isRefreshing = false, filtersSummary = summary(null)))
        private set

    var filtersState by mutableStateOf<FiltersState?>(null)
        private set

    private var loadJob: Job? = null

    init {
        load()
    }

    fun refresh() {
        state = state.copy(isRefreshing = true)
        load()
    }

    fun retry() {
        state = state.copy(result = null, isRefreshing = false)
        load()
    }

    fun openFilters() {
        filtersState = FiltersState(repository.currentFilters(), locationDenied = false, pendingDistanceKm = null)
    }

    fun editFilters(change: (SearchFilters) -> SearchFilters) {
        filtersState = filtersState?.let { it.copy(filters = change(it.filters)) }
    }

    fun resetFilters() {
        editFilters { repository.defaultFilters }
    }

    fun chooseArea(area: SearchArea) {
        if (area is SearchArea.Nearby && !locationProvider.hasPermission()) {
            filtersState = filtersState?.copy(pendingDistanceKm = area.km)
        } else {
            editFilters { it.withArea(area) }
        }
    }

    fun dismissLocationRequest() {
        filtersState = filtersState?.copy(pendingDistanceKm = null)
    }

    fun onLocationPermissionResult(granted: Boolean) {
        val current = filtersState ?: return
        val pending = current.pendingDistanceKm
        filtersState = if (granted && pending != null) {
            current.copy(filters = current.filters.withArea(SearchArea.Nearby(pending)), pendingDistanceKm = null, locationDenied = false)
        } else {
            current.copy(pendingDistanceKm = null, locationDenied = !granted)
        }
    }

    fun closeFilters() {
        val edited = filtersState?.filters ?: return
        filtersState = null
        if (edited != repository.currentFilters()) {
            repository.saveFilters(edited)
            state = state.copy(result = null, isRefreshing = false)
            load()
        }
    }

    fun onReturnToApp() {
        filtersState = filtersState?.let { it.copy(locationDenied = it.locationDenied && !locationProvider.hasPermission()) }
        refresh()
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val position = locationProvider.currentPosition()
            val result = repository.loadListings(position)
            state = ListingsState(result = result, isRefreshing = false, filtersSummary = summary(position))
        }
    }

    private fun summary(position: GeoPoint?): String =
        repository.filtersSummary(repository.currentFilters(), hasPosition = position != null)
}
