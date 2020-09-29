package no.nav.pgi.skatt.leshendelse.maskinporten

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.JWTClaimsSet.Builder
import no.nav.pgi.skatt.leshendelse.getVal
import java.util.*

const val SCOPE_CLAIM = "scope"

const val AUDIENCE_ENV_KEY = "aud_maskinporten"
const val ISSUER_ENV_KEY = "iss_maskinporten"
const val SCOPE_ENV_KEY = "scope_maskinporten"
const val VALID_IN_SECONDS_ENV_KEY = "jwt_expiration_time_seconds_maskinporten"

const val ONE_SECOND_IN_MILLISECONDS = 1000

internal class MaskinportenClaims(private val env: Map<String, String>) {
    private val audience = env.getVal(AUDIENCE_ENV_KEY)
    private val issuer = env.getVal(ISSUER_ENV_KEY)
    private val scope = env.getVal(SCOPE_ENV_KEY)

    fun createJwtClaimSet(): JWTClaimsSet = Builder().apply {
        audience(audience)
        issuer(issuer)
        claim(SCOPE_CLAIM, scope)
        issueTime(Date())
        expirationTime(Date() addSeconds env.getVal(VALID_IN_SECONDS_ENV_KEY).toInt())
    }.build()

    companion object {
        internal fun requiredEnvKeys() = listOf(AUDIENCE_ENV_KEY, ISSUER_ENV_KEY, SCOPE_ENV_KEY, VALID_IN_SECONDS_ENV_KEY)
    }
}

internal infix fun Date.addSeconds(seconds: Int): Date = Date(this.time + seconds * ONE_SECOND_IN_MILLISECONDS)
