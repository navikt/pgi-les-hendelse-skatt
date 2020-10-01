package no.nav.pgi.skatt.leshendelse.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse


internal const val HENDELSE_PORT = 8085
internal const val HENDELSE_PATH = "/api/skatteoppgjoer/ekstern/grunnlag-pgi/hendelse"
internal const val HENDELSE_URL = "http://localhost:$HENDELSE_PORT$HENDELSE_PATH"

internal class HendelseMock {

    private val skattApiMock = WireMockServer(HENDELSE_PORT)

    init {
        skattApiMock.start()
    }

    internal fun `stub hendelse endepunkt skatt`() {
        skattApiMock.stubFor(WireMock.get(WireMock.urlPathEqualTo(HENDELSE_PATH)).willReturn(
                aResponse()
                        .withBodyFile("Hendelser1To100.json")
                        .withStatus(200)
        ))
    }

    internal fun stop() {
        skattApiMock.stop()
    }
}