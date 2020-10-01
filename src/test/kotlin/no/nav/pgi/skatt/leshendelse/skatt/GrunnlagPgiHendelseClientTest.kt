package no.nav.pgi.skatt.leshendelse.skatt

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import no.nav.pgi.skatt.leshendelse.maskinporten.*
import no.nav.pgi.skatt.leshendelse.maskinporten.mock.MASKINPORTEN_MOCK_HOST
import no.nav.pgi.skatt.leshendelse.maskinporten.mock.MaskinportenMock
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals

private const val PORT = 8085
private const val PATH = "/testpath"
private const val URL = "http://localhost:$PORT$PATH"

private const val ANTALL_KEY = "antall"
private const val FRA_SEKVENSNUMMER_KEY = "fraSekvensnummer"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GrunnlagPgiHendelseClientTest {
    private val hendelseMock = WireMockServer(PORT)
    private val maskinportenMock = MaskinportenMock()
    private val client = GrunnlagPgiHendelseClient(createEnvVariables())

    @BeforeAll
    internal fun init() {
        hendelseMock.start()
        maskinportenMock.mockMaskinporten()
    }

    @AfterAll
    internal fun teardown() {
        hendelseMock.stop()
        maskinportenMock.close()
    }

    @Test
    fun `returns hendelser`() {
        val antall = 1
        val fraSekvensnummer = 1L
        `stub valid response`(antall, fraSekvensnummer)

        val hendelser = client.send(antall, fraSekvensnummer)

        assertEquals(hendelser.size(), 5)
    }

    @Test
    fun `throw exception when nestesekvensnr is missing from response`() {
        val antall = 2
        val fraSekvensnummer = 1L
        `stub response without nestesekvensnr`(antall, fraSekvensnummer)

        assertThrows<GrunnlagPgiHendelserValidationException> {
            client.send(antall, fraSekvensnummer)
        }
    }

    @Test
    fun `throw exception when response is not mappable`() {
        val antall = 3
        val fraSekvensnummer = 1L
        `stub response that wont map`(antall, fraSekvensnummer)

        assertThrows<GrunnlagPgiHendelseClientObjectMapperException> {
            client.send(antall, fraSekvensnummer)
        }
    }

    private fun `stub valid response`(antall: Int, fraSekvensnummer: Long) {
        hendelseMock.stubFor(WireMock.get(WireMock.urlPathEqualTo(PATH))
                .withQueryParams(
                        mapOf(
                                ANTALL_KEY to equalTo((antall.toString())),
                                FRA_SEKVENSNUMMER_KEY to equalTo((fraSekvensnummer.toString()))
                        )
                )
                .willReturn(
                        WireMock.aResponse()
                                .withBodyFile("Hendelser1To100.json")
                                .withStatus(200)
                ))
    }

    private fun `stub response without nestesekvensnr`(antall: Int, fraSekvensnummer: Long) {
        hendelseMock.stubFor(WireMock.get(WireMock.urlPathEqualTo(PATH))
                .withQueryParams(
                        mapOf(
                                ANTALL_KEY to equalTo((antall.toString())),
                                FRA_SEKVENSNUMMER_KEY to equalTo((fraSekvensnummer.toString()))
                        )
                )
                .willReturn(
                        WireMock.aResponse()
                                .withBody("{}")
                                .withStatus(200)
                ))
    }

    private fun `stub response that wont map`(antall: Int, fraSekvensnummer: Long) {
        hendelseMock.stubFor(WireMock.get(WireMock.urlPathEqualTo(PATH))
                .withQueryParams(
                        mapOf(
                                ANTALL_KEY to equalTo((antall.toString())),
                                FRA_SEKVENSNUMMER_KEY to equalTo((fraSekvensnummer.toString()))
                        )
                )
                .willReturn(
                        WireMock.aResponse()
                                .withBody("[")
                                .withStatus(200)
                ))
    }

    private fun createEnvVariables() = mapOf(
            AUDIENCE_ENV_KEY to "testAud",
            ISSUER_ENV_KEY to "testIssuer",
            SCOPE_ENV_KEY to "testScope",
            VALID_IN_SECONDS_ENV_KEY to "120",
            PRIVATE_JWK_ENV_KEY to RSAKeyGenerator(2048).keyID("123").generate().toJSONString(),
            MASKINPORTEN_TOKEN_HOST_ENV_KEY to MASKINPORTEN_MOCK_HOST,
            HENDELSE_SKATT_URL_KEY to URL
    )

}