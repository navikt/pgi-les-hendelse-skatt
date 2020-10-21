package no.nav.pgi.skatt.leshendelse.maskinporten

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import no.nav.pensjon.samhandling.env.getVal
import java.util.*

internal const val SCOPE_CLAIM = "scope"
internal const val ONE_SECOND_IN_MILLISECONDS = 1000

internal const val PRIVATE_JWK_ENV_KEY = "JWK_PRIVATE_KEY"
internal const val AUDIENCE_ENV_KEY = "AUD_MASKINPORTEN"
internal const val ISSUER_ENV_KEY = "ISS_MASKINPORTEN"
internal const val SCOPE_ENV_KEY = "SCOPE_MASKINPORTEN"
internal const val VALID_IN_SECONDS_ENV_KEY = "JWT_EXPIRATION_TIME_SECONDS_MASKINPORTEN"


internal class MaskinportenGrantTokenGenerator(env: Map<String, String>) {
    private val privateKey: RSAKey = RSAKey.parse(env.getVal(PRIVATE_JWK_ENV_KEY))
    private val audience = env.getVal(AUDIENCE_ENV_KEY)
    private val issuer = env.getVal(ISSUER_ENV_KEY)
    private val scope = env.getVal(SCOPE_ENV_KEY)
    private val validInSecond = env.getVal(VALID_IN_SECONDS_ENV_KEY).toInt()

    internal fun generateJwt(): String = SignedJWT(signatureHeader(), jwtClaimSet())
            .apply { sign(RSASSASigner(privateKey)) }
            .serialize()

    private fun signatureHeader() = JWSHeader.Builder(JWSAlgorithm.RS256).keyID(privateKey.keyID).build()

    private fun jwtClaimSet(): JWTClaimsSet = JWTClaimsSet.Builder().apply {
        audience(audience)
        issuer(issuer)
        claim(SCOPE_CLAIM, scope)
        issueTime(Date())
        expirationTime(Date() addSeconds validInSecond)
    }.build()

    companion object {
        internal fun requiredEnvKeys() = listOf(PRIVATE_JWK_ENV_KEY, AUDIENCE_ENV_KEY, ISSUER_ENV_KEY, SCOPE_ENV_KEY, VALID_IN_SECONDS_ENV_KEY)
    }
}

internal infix fun Date.addSeconds(seconds: Int): Date = Date(this.time + seconds * ONE_SECOND_IN_MILLISECONDS)
