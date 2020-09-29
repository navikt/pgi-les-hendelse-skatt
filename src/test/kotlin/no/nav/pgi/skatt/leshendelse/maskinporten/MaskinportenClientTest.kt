package no.nav.pgi.skatt.leshendelse.maskinporten

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.*


private const val PORT = 8085
private const val HOST = "http://localhost:$PORT"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MaskinportenClientTest() {
    private lateinit var maskinportenClient: MaskinportenClient
    private val endpointMock = WireMockServer(PORT)
    private val privateKey: RSAKey = RSAKeyGenerator(2048).keyID("123").generate()

    @BeforeAll
    internal fun init() {
        endpointMock.start()
        maskinportenClient = MaskinportenClient(createEnvVariables())
    }

    @AfterAll
    internal fun teardown() {
        endpointMock.stop()
    }

    @AfterEach
    internal fun afterEach() {
        endpointMock.resetAll()
    }

    @Test
    fun `reuse token if not expired`() {
        mockMaskinporten()

        val firstToken = maskinportenClient.getToken()
        val secondToken = maskinportenClient.getToken()
        assertEquals(firstToken, secondToken)
    }


    private fun createEnvVariables() = mapOf(
            AUDIENCE_ENV_KEY to "testAud",
            ISSUER_ENV_KEY to "testIssuer",
            SCOPE_ENV_KEY to "testScope",
            VALID_IN_SECONDS_ENV_KEY to "120",
            PRIVATE_JWK_ENV_KEY to privateKey.toJSONString(),
            MASKINPORTEN_CREATE_TOKEN_HOST_ENV_KEY to HOST
    )

    private fun mockMaskinporten(token: String) {
        endpointMock.stubFor(post(urlPathEqualTo(MASKINPORTEN_CREATE_TOKEN_PATH))
                .withHeader("Content-Type", equalTo(CONTENT_TYPE))
                .inScenario("First time")
                .whenScenarioStateIs(STARTED)

                .withRequestBody(matchingJsonPath("$.grant_type", matching(GRANT_TYPE)))
                .withRequestBody(matchingJsonPath("$.assertion"))
                .willReturn(ok("""{
                      "access_token" : "$token",
                      "token_type" : "Bearer",
                      "expires_in" : 599,
                      "scope" : "difitest:test1"
                    }
                """))
                .willSetStateTo("Retry"))
    }

    private fun mockMaskinporten() {
        endpointMock.stubFor(post(urlPathEqualTo(MASKINPORTEN_CREATE_TOKEN_PATH))
                .withHeader("Content-Type", equalTo(CONTENT_TYPE))
                .inScenario("First time")
                .whenScenarioStateIs(STARTED)

                .withRequestBody(matchingJsonPath("$.grant_type", matching(GRANT_TYPE)))
                .withRequestBody(matchingJsonPath("$.assertion"))
                .willReturn(ok("""{
                      "access_token" : "${createMaskinportenToken()}",
                      "token_type" : "Bearer",
                      "expires_in" : 599,
                      "scope" : "difitest:test1"
                    }
                """))
                .willSetStateTo("Retry"))
    }

    private fun createMaskinportenToken(): String {
        val claimsSet = JWTClaimsSet.Builder()
                .subject("alice")
                .issuer("https://c2id.com")
                .expirationTime(Date(Date().getTime() + 60 * 1000))
                .build()
        val signedJWT = SignedJWT(
                JWSHeader.Builder(JWSAlgorithm.RS256).keyID(privateKey.getKeyID()).build(),
                claimsSet)
        val signer: JWSSigner = RSASSASigner(privateKey)
        signedJWT.sign(signer)
        return signedJWT.serialize()
    }
}