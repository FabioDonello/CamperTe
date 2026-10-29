package camperte.sources

import camperte.Config
import camperte.Listing

interface Source {
    val name: String

    suspend fun search(config: Config): List<Listing>
}

internal fun activeSources(): List<Source> {
    val client = createHttpClient()
    return listOf(SubitoSource(client), CamperOnLineSource(client))
}
