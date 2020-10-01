package no.nav.pgi.skatt.leshendelse.skatt

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import no.nav.pgi.skatt.leshendelse.maskinporten.*
import no.nav.pgi.skatt.leshendelse.maskinporten.mock.MASKINPORTEN_MOCK_HOST
import no.nav.pgi.skatt.leshendelse.maskinporten.mock.MaskinportenMock
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.net.http.HttpResponse


private const val PORT = 8086
private const val PATH = "/testpath"
private const val URL = "http://localhost:$PORT$PATH"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SkattClientTest {
    private val skattClient: SkattClient = SkattClient(createEnvVariables())
    private val skattMock = WireMockServer(PORT)
    private val maskinportenMock = MaskinportenMock()

    @BeforeAll
    internal fun init() {
        skattMock.start()
        maskinportenMock.mockMaskinporten()
    }

    @AfterAll
    internal fun teardown() {
        skattMock.stop()
        maskinportenMock.close()
    }

    @Test
    fun `createGetRequest should add query parameters`() {
        val key1 = "key1"
        val value1 = "value1"
        val key2 = "key2"
        val value2 = "value2"

        skattMock.stubFor(WireMock.get(WireMock.urlPathEqualTo(PATH))
                .withQueryParams(mapOf(
                        key1 to WireMock.equalTo((value1)),
                        key2 to WireMock.equalTo((value2))
                ))
                .willReturn(WireMock.ok()))
        val response = skattClient.send(skattClient.createGetRequest(URL, mapOf(key1 to value1, key2 to value2)), HttpResponse.BodyHandlers.discarding())

        assertEquals(response.statusCode(),200)
    }

    private fun createEnvVariables() = mapOf(
            AUDIENCE_ENV_KEY to "testAud",
            ISSUER_ENV_KEY to "testIssuer",
            SCOPE_ENV_KEY to "testScope",
            VALID_IN_SECONDS_ENV_KEY to "120",
            PRIVATE_JWK_ENV_KEY to RSAKeyGenerator(2048).keyID("123").generate().toJSONString(),
            MASKINPORTEN_TOKEN_HOST_ENV_KEY to MASKINPORTEN_MOCK_HOST
    )
}