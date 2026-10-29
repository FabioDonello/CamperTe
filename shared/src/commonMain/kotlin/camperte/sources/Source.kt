package camperte.sources

import camperte.Config
import camperte.Listing

interface Source {
    val name: String

    suspend fun search(config: Config): List<Listing>
}

internal fun activeSources(): List<Source> = listOf(SubitoSource(createHttpClient()))
