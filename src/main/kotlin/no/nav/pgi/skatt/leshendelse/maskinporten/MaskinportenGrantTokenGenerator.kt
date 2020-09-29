package no.nav.pgi.skatt.leshendelse.maskinporten

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.SignedJWT
import no.nav.pgi.skatt.leshendelse.getVal
import no.nav.pgi.skatt.leshendelse.verify

internal const val PRIVATE_JWK_ENV_KEY = "jwk-private-key"

internal class MaskinportenGrantTokenGenerator(env: Map<String, String>) {
    private val claims: MaskinportenClaims
    private val privateKey: RSAKey
    private val jwsSigner: JWSSigner
    private val jwsAlgorithm = JWSAlgorithm.RS256

    init {
        env.verify(MaskinportenClaims.requiredEnvKeys() + PRIVATE_JWK_ENV_KEY)

        claims = MaskinportenClaims(env)
        privateKey = RSAKey.parse(env.getVal(PRIVATE_JWK_ENV_KEY))
        jwsSigner = RSASSASigner(privateKey)
    }

    fun createJwt(): String =
            SignedJWT(signatureHeader(), claims.createJwtClaimSet()).apply {
                sign(jwsSigner)
            }.serialize()


    private fun signatureHeader() = JWSHeader.Builder(jwsAlgorithm).keyID(privateKey.keyID).build()

}



