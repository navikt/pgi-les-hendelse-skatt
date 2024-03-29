package no.nav.pgi.skatt.leshendelse.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.matching.AnythingPattern

internal const val FIRST_SEKVENSNUMMER_PORT = 8087
internal const val FIRST_SEKVENSNUMMER_MOCK_HOST = "http://localhost:$FIRST_SEKVENSNUMMER_PORT"
internal const val FIRST_SEKVENSNUMMER_MOCK_PATH =
    "/api/formueinntekt/pensjonsgivendeinntektforfolketrygden/hendelser/start"

internal const val SKATT_FIRST_SEKVENSNUMMER = 1L

internal class SkattFirstSekvensnummerMock {

    private val mock = WireMockServer(FIRST_SEKVENSNUMMER_PORT)

    init {
        mock.start()
    }

    internal fun reset() {
        mock.resetAll()
    }

    internal fun stop() = mock.stop()

    internal fun `stub first sekvensnummer endpoint`(firstSekvensnummer: Long = SKATT_FIRST_SEKVENSNUMMER) {
        mock.stubFor(
            get(urlPathEqualTo(FIRST_SEKVENSNUMMER_MOCK_PATH))
                .willReturn(okJson("{\"sekvensnummer\": $firstSekvensnummer}"))
        )
    }

    internal fun `stub first sekvensnummer endpoint med dato`(firstSekvensnummer: Long = SKATT_FIRST_SEKVENSNUMMER) {
        mock.stubFor(
            get(urlPathEqualTo(FIRST_SEKVENSNUMMER_MOCK_PATH)).withQueryParam("dato", AnythingPattern())
                .willReturn(okJson("{\"sekvensnummer\": $firstSekvensnummer}"))
        )
    }

    internal fun `mock 404 response`() {
        mock.stubFor(
            get(urlPathEqualTo(FIRST_SEKVENSNUMMER_MOCK_PATH))
                .willReturn(notFound().withBody("Test message"))
        )
    }

    internal fun `mock faulty json response`() {
        mock.stubFor(
            get(urlPathEqualTo(FIRST_SEKVENSNUMMER_MOCK_PATH))
                .willReturn(okJson("{//}"))
        )
    }

    internal fun `mock response without first sekvensnummer`() {
        mock.stubFor(
            get(urlPathEqualTo(FIRST_SEKVENSNUMMER_MOCK_PATH))
                .willReturn(okJson("{}"))
        )
    }
}