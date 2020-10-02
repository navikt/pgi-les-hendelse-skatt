package no.nav.pgi.skatt.leshendelse.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import no.nav.pgi.skatt.leshendelse.skatt.FIRST_SEKVENSNUMMER_PATH
import no.nav.pgi.skatt.leshendelse.skatt.HENDELSE_PATH

internal const val FIRST_SEKVENSNUMMER_PORT = 8084
internal const val FIRST_SEKVENSNUMMER_MOCK_HOST = "http://localhost:$FIRST_SEKVENSNUMMER_PORT"

internal const val SKATT_FIRST_SEKVENSNUMMER = 1

internal class FirstSekvensnummerMock {

    private val skattApiMock = WireMockServer(FIRST_SEKVENSNUMMER_PORT)

    init {
        skattApiMock.start()
    }

    internal fun `stub first sekvensnummer endepunkt skatt`() {
        skattApiMock.stubFor(get(urlPathEqualTo(FIRST_SEKVENSNUMMER_PATH))
                .willReturn(okJson("{\"sekvensnummer\": " + SKATT_FIRST_SEKVENSNUMMER + "}")))
    }

    internal fun stop() = skattApiMock.stop()
}

