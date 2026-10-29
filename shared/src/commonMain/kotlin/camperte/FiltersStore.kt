package camperte

import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json

internal class FiltersStore(private val settings: Settings, private val defaults: SearchFilters) {

    fun load(): SearchFilters {
        val saved = settings.getStringOrNull(KEY) ?: return defaults
        return try {
            Json.decodeFromString<SearchFilters>(saved)
        } catch (e: IllegalArgumentException) {
            defaults
        }
    }

    fun save(filters: SearchFilters) {
        settings.putString(KEY, Json.encodeToString(filters))
    }

    private companion object {
        const val KEY = "search_filters"
    }
}
