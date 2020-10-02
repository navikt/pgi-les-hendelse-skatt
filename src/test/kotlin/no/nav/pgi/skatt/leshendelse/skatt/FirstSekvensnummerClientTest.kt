package no.nav.pgi.skatt.leshendelse.skatt

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.pgi.skatt.leshendelse.mock.MaskinportenMock
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

private const val PORT = 8086
private const val HOST = "http://localhost:$PORT"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FirstSekvensnummerClientTest {
    private val hendelseMock = WireMockServer(PORT)
    private val maskinportenMock = MaskinportenMock()
    private val client = FirstSekvensnummerClient(HOST)

    @BeforeAll
    internal fun init() {
        hendelseMock.start()
    }

    @AfterAll
    internal fun teardown() {
        hendelseMock.stop()
    }

    private fun `stub response without nestesekvensnr`(antall: Int, fraSekvensnummer: Long) {
        hendelseMock.stubFor(WireMock.get(WireMock.urlPathEqualTo(FIRST_SEKVENSNUMMER_PATH))
                .willReturn(
                        WireMock.aResponse()
                                .withBody("""{"nesteSekvensnummer": 1}""")
                                .withStatus(200)
                ))
    }
}