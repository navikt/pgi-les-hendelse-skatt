package no.nav.pgi.skatt.leshendelse

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*

internal const val SKATT_API_PORT = 8084
internal const val SKATT_FIRST_HENDELSE_URL = "/api/skatteoppgjoer/ekstern/grunnlag-pgi/hendelse/start"

internal class SekvensnummerMock {

    private val skattApiMock = WireMockServer(SKATT_API_PORT)

    init {
        skattApiMock.start()
    }


    internal fun `stub first sekvensnummer from skatt`() {
        skattApiMock.stubFor(get(urlPathEqualTo(SKATT_FIRST_HENDELSE_URL))
                .willReturn(ok()))
    }

    internal fun stop() {
        skattApiMock.stop()
    }
}
