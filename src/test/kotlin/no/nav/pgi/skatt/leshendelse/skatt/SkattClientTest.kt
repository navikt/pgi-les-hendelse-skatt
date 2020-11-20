package no.nav.pgi.skatt.leshendelse.skatt

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.nimbusds.jwt.SignedJWT
import no.nav.pgi.skatt.leshendelse.mock.MaskinportenMock
import no.nav.pgi.skatt.leshendelse.mock.MaskinportenMock.Companion.MASKINPORTEN_ENV_VARIABLES
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.net.http.HttpResponse


private const val PORT = 8086
private const val PATH = "/testpath"
private const val URL = "http://localhost:$PORT$PATH"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SkattClientTest {
    private val skattClient: SkattClient = SkattClient(MASKINPORTEN_ENV_VARIABLES)
    private val skattMock = WireMockServer(PORT)
    private val maskinportenMock = MaskinportenMock()

    @BeforeAll
    internal fun init() {
        skattMock.start()
        maskinportenMock.`mock  maskinporten token enpoint`()
    }

    @AfterAll
    internal fun teardown() {
        skattMock.stop()
        maskinportenMock.stop()
    }

    @Test
    fun `httpRequest should have bearer token in Authorization header`() {
        val httpRequest = skattClient.createGetRequest(URL, mapOf("key" to "value"))
        val authorizationHeader = httpRequest.headers().firstValue("Authorization").get()
        assertTrue(authorizationHeader containsRegex """Bearer\s.*""")
        assertDoesNotThrow { parseJwt(authorizationHeader) }
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

        assertEquals(response.statusCode(), 200)
    }

    private fun parseJwt(bearerToken: String) = SignedJWT.parse(bearerToken.split("""Bearer """)[1])
    private infix fun String.containsRegex(regex: String): Boolean = regex.toRegex().matches(this)
}