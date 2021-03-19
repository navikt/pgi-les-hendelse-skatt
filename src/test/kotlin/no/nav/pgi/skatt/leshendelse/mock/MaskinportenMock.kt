package no.nav.pgi.skatt.leshendelse.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import no.nav.pensjon.opptjening.gcp.maskinporten.client.config.MaskinportenEnvVariableConfigCreator.Companion.MASKINPORTEN_CLIENT_ID_KEY
import no.nav.pensjon.opptjening.gcp.maskinporten.client.config.MaskinportenEnvVariableConfigCreator.Companion.MASKINPORTEN_HOST_KEY
import no.nav.pensjon.opptjening.gcp.maskinporten.client.config.MaskinportenEnvVariableConfigCreator.Companion.MASKINPORTEN_JWK_PRIVATE_KEY
import no.nav.pensjon.opptjening.gcp.maskinporten.client.config.MaskinportenEnvVariableConfigCreator.Companion.MASKINPORTEN_JWT_EXPIRATION_TIME_IN_SECONDS_KEY
import no.nav.pensjon.opptjening.gcp.maskinporten.client.config.MaskinportenEnvVariableConfigCreator.Companion.MASKINPORTEN_SCOPE_KEY
import java.util.*

private const val PORT = 8096
private const val TOKEN_PATH = "/token"
internal const val MASKINPORTEN_MOCK_HOST = "http://localhost:$PORT"

internal class MaskinportenMock {
    private var mock = WireMockServer(PORT)
    private val privateKey: RSAKey = RSAKeyGenerator(2048).keyID("123").generate()

    init {
        mock.start()
    }

    internal fun stop() {
        mock.stop()
    }

    internal fun `mock maskinporten token enpoint`() {
        mock.stubFor(WireMock.post(WireMock.urlPathEqualTo(TOKEN_PATH))
                .willReturn(WireMock.ok("""{
                      "access_token" : "${createMaskinportenToken()}",
                      "token_type" : "Bearer",
                      "expires_in" : 599,
                      "scope" : "difitest:test1"
                    }
                """)))
    }

    private fun createMaskinportenToken(): String {
        val claimsSet = JWTClaimsSet.Builder()
                .subject("alice")
                .issuer("https://c2id.com")
                .expirationTime(Date(Date().getTime() + (60 * 1000)))
                .build()
        val signedJWT = SignedJWT(
                JWSHeader.Builder(JWSAlgorithm.RS256).keyID(privateKey.getKeyID()).build(),
                claimsSet)
        val signer: JWSSigner = RSASSASigner(privateKey)
        signedJWT.sign(signer)
        return signedJWT.serialize()
    }

    companion object {
        val MASKINPORTEN_ENV_VARIABLES: Map<String, String> = mapOf(
            MASKINPORTEN_SCOPE_KEY to "testScope",
            MASKINPORTEN_CLIENT_ID_KEY to "testClient",
            MASKINPORTEN_JWT_EXPIRATION_TIME_IN_SECONDS_KEY to "120",
            MASKINPORTEN_JWK_PRIVATE_KEY to RSAKeyGenerator(2048).keyID("123").generate().toJSONString(),
            MASKINPORTEN_HOST_KEY to MASKINPORTEN_MOCK_HOST
        )
    }
}