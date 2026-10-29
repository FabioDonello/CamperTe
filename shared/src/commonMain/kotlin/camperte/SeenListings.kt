package camperte

import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

internal class SeenListings(private val settings: Settings) {

    fun recordShown(ids: List<String>, now: Instant): Set<String> {
        val nowMillis = now.toEpochMilliseconds()
        val kept = load().filterValues { nowMillis - it.lastShownMillis <= RETENTION.inWholeMilliseconds }
        val shown = ids.associateWith { id ->
            Sighting(firstShownMillis = kept[id]?.firstShownMillis ?: nowMillis, lastShownMillis = nowMillis)
        }
        settings.putString(KEY, Json.encodeToString(kept + shown))
        return shown.filterValues { nowMillis - it.firstShownMillis < NEW_FOR.inWholeMilliseconds }.keys
    }

    private fun load(): Map<String, Sighting> {
        val saved = settings.getStringOrNull(KEY) ?: return emptyMap()
        return try {
            Json.decodeFromString(saved)
        } catch (e: IllegalArgumentException) {
            emptyMap()
        }
    }

    @Serializable
    private data class Sighting(val firstShownMillis: Long, val lastShownMillis: Long)

    private companion object {
        const val KEY = "seen_listings"
        val NEW_FOR = 12.hours
        val RETENTION = 90.days
    }
}
