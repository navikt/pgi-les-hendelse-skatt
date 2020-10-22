package no.nav.pgi.skatt.leshendelse.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.Scenario
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.util.*

private const val PORT = 8096
private const val TOKEN_PATH = "/token"
internal const val MASKINPORTEN_MOCK_HOST = "http://localhost:$PORT"
internal const val GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer"
internal const val CONTENT_TYPE = "application/x-www-form-urlencoded"

internal class MaskinportenMock {
    private var mock = WireMockServer(PORT)
    private val privateKey: RSAKey = RSAKeyGenerator(2048).keyID("123").generate()

    init {
        mock.start()
    }

    internal fun reset() {
        mock.resetAll()
    }

    internal fun stop() {
        mock.stop()
    }

    internal fun `mock  maskinporten token enpoint`() {
        mock.stubFor(WireMock.post(WireMock.urlPathEqualTo(TOKEN_PATH))
                .willReturn(WireMock.ok("""{
                      "access_token" : "${createMaskinportenToken()}",
                      "token_type" : "Bearer",
                      "expires_in" : 599,
                      "scope" : "difitest:test1"
                    }
                """)))
    }

    internal fun `mock valid response for only one call`() {
        mock.stubFor(WireMock.post(WireMock.urlPathEqualTo(TOKEN_PATH))
                .inScenario("First time")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(WireMock.ok("""{
                      "access_token" : "${createMaskinportenToken()}",
                      "token_type" : "Bearer",
                      "expires_in" : 599,
                      "scope" : "difitest:test1"
                    }
                """))
                .willSetStateTo("Ended"))
    }

    internal fun `mock invalid JSON response`() {
        mock.stubFor(WireMock.post(WireMock.urlPathEqualTo(TOKEN_PATH))
                .willReturn(WireMock.ok("""w""")))
    }

    internal fun `mock 500 server error`() {
        mock.stubFor(WireMock.post(WireMock.urlPathEqualTo(TOKEN_PATH))
                .willReturn(WireMock.serverError().withBody("test body")))
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
}