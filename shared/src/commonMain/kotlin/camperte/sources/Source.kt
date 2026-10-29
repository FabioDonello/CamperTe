package camperte.sources

import camperte.Config
import camperte.Listing

interface Source {
    val name: String

    suspend fun search(config: Config): List<Listing>
}

internal val activeSources: List<Source> = listOf(SampleSource())
