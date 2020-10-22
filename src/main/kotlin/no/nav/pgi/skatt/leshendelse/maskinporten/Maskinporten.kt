package no.nav.pgi.skatt.leshendelse.maskinporten

import com.nimbusds.jose.jwk.RSAKey
import no.nav.pensjon.samhandling.env.getVal
import no.nav.pensjon.samhandling.env.verifyEnvironmentVariables
import no.nav.security.maskinporten.client.MaskinportenClient
import no.nav.security.maskinporten.client.MaskinportenConfig

internal const val MASKINPORTEN_TOKEN_HOST_ENV_KEY = "MASKINPORTEN_HOST"
internal const val CLIENT_ID_ENV_KEY = "MASKINPORTEN_CLIENT_ID"
internal const val PRIVATE_JWK_ENV_KEY = "MASKINPORTEN_JWK_PRIVATE_KEY"
internal const val SCOPE_ENV_KEY = "MASKINPORTEN_SCOPE"
internal const val VALID_IN_SECONDS_ENV_KEY = "MASKINPORTEN_JWT_EXPIRATION_TIME_IN_SECONDS"

internal class Maskinporten(env: Map<String, String>) {
    private val requiredEnvKey = listOf(MASKINPORTEN_TOKEN_HOST_ENV_KEY, CLIENT_ID_ENV_KEY, PRIVATE_JWK_ENV_KEY, SCOPE_ENV_KEY, VALID_IN_SECONDS_ENV_KEY)
    private val maskinportenClient: MaskinportenClient = MaskinportenClient(createMaskinportenConfig(env))
    internal val token: String get() = maskinportenClient.maskinportenTokenString

    private fun createMaskinportenConfig(env: Map<String, String>): MaskinportenConfig {
        env.verifyEnvironmentVariables(requiredEnvKey)
        return MaskinportenConfig(
                baseUrl = env.getVal(MASKINPORTEN_TOKEN_HOST_ENV_KEY),
                clientId = env.getVal(CLIENT_ID_ENV_KEY),
                privateKey = RSAKey.parse(env.getVal(PRIVATE_JWK_ENV_KEY)),
                scope = env.getVal(SCOPE_ENV_KEY),
                validInSeconds = env.getVal(VALID_IN_SECONDS_ENV_KEY).toInt()
        )
    }
}