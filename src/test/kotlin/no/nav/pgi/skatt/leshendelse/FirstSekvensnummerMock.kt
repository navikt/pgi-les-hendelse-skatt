package no.nav.pgi.skatt.leshendelse

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*

internal const val SKATT_API_PORT = 8084
internal const val SKATT_FIRST_HENDELSE_URL = "/api/skatteoppgjoer/ekstern/grunnlag-pgi/hendelse/start"
internal const val SKATT_FIRST_SEKVENSNUMMER = 1

internal class FirstSekvensnummerMock {


    private val skattApiMock = WireMockServer(SKATT_API_PORT)

    init {
        skattApiMock.start()
    }

    // region Stubs
    internal fun `stub first sekvensnummer endepunkt skatt`() {
        skattApiMock.stubFor(get(urlPathEqualTo(SKATT_FIRST_HENDELSE_URL))
                .willReturn(okJson("{\"sekvensnummer\": " + SKATT_FIRST_SEKVENSNUMMER + "}")))
    }
    // endregion

    internal fun stop() {
        skattApiMock.stop()
    }
}

