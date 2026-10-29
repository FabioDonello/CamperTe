package camperte.sources

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent

private const val MOBILE_BROWSER_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; SM-A556B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"

internal fun createHttpClient(): HttpClient = HttpClient {
    expectSuccess = true
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = 20_000
    }
    install(UserAgent) {
        agent = MOBILE_BROWSER_USER_AGENT
    }
}
