package camperte.android

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import camperte.CamperRepository
import camperte.SearchResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class ListingsState(val result: SearchResult?, val isRefreshing: Boolean)

class ListingsViewModel(private val repository: CamperRepository = CamperRepository()) : ViewModel() {

    var state by mutableStateOf(ListingsState(result = null, isRefreshing = false))
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
        state = ListingsState(result = null, isRefreshing = false)
        load()
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            state = ListingsState(result = repository.loadListings(), isRefreshing = false)
        }
    }
}
