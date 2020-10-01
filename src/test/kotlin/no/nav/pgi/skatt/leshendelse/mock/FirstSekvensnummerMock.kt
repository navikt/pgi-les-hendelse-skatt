package no.nav.pgi.skatt.leshendelse.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*

internal const val FIRST_SEKVENSNUMMER_PORT = 8084
internal const val SKATT_FIRST_SEKVENSNUMMER_PATH = "/api/skatteoppgjoer/ekstern/grunnlag-pgi/hendelse/start"
internal const val FIRST_SEKVENSNUMMER_URL = "http://localhost:$FIRST_SEKVENSNUMMER_PORT$SKATT_FIRST_SEKVENSNUMMER_PATH"

internal const val SKATT_FIRST_SEKVENSNUMMER = 1

internal class FirstSekvensnummerMock {

    private val skattApiMock = WireMockServer(FIRST_SEKVENSNUMMER_PORT)

    init {
        skattApiMock.start()
    }

    internal fun `stub first sekvensnummer endepunkt skatt`() {
        skattApiMock.stubFor(get(urlPathEqualTo(SKATT_FIRST_SEKVENSNUMMER_PATH))
                .willReturn(okJson("{\"sekvensnummer\": " + SKATT_FIRST_SEKVENSNUMMER + "}")))
    }

    internal fun stop() = skattApiMock.stop()
}

