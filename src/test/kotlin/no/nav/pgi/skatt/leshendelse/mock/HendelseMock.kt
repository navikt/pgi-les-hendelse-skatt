package no.nav.pgi.skatt.leshendelse.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import no.nav.pgi.skatt.leshendelse.skatt.HENDELSE_PATH


private const val ANTALL_KEY = "antall"
private const val FRA_SEKVENSNUMMER_KEY = "fraSekvensnummer"

private const val HENDELSE_PORT = 8085
internal const val HENDELSE_MOCK_HOST = "http://localhost:$HENDELSE_PORT"

internal class HendelseMock {

    private val skattApiHendelseMock = WireMockServer(HENDELSE_PORT)

    init {
        skattApiHendelseMock.start()
    }

    internal fun `stub hendelse endepunkt skatt`() {
        skattApiHendelseMock.stubFor(WireMock.get(WireMock.urlPathEqualTo(HENDELSE_PATH))
                .willReturn(
                        aResponse()
                                .withBodyFile("Hendelser1To100.json")
                                .withStatus(200)
                ))
    }

    internal fun `stub for skatt hendelser`(antall: Int, fraSekvensnummer: Long) {
        skattApiHendelseMock.stubFor(WireMock.get(WireMock.urlPathEqualTo(HENDELSE_PATH))
                .withQueryParams(
                        mapOf(
                                ANTALL_KEY to WireMock.equalTo((antall.toString())),
                                FRA_SEKVENSNUMMER_KEY to WireMock.equalTo((fraSekvensnummer.toString()))
                        )
                )
                .willReturn(
                        WireMock.aResponse()
                                .withBodyFile("Hendelser1To100.json")
                                .withStatus(200)
                ))
    }

    internal fun `stub response without nestesekvensnr`(antall: Int, fraSekvensnummer: Long) {
        skattApiHendelseMock.stubFor(WireMock.get(WireMock.urlPathEqualTo(HENDELSE_PATH))
                .withQueryParams(
                        mapOf(
                                ANTALL_KEY to WireMock.equalTo((antall.toString())),
                                FRA_SEKVENSNUMMER_KEY to WireMock.equalTo((fraSekvensnummer.toString()))
                        )
                )
                .willReturn(
                        aResponse()
                                .withBody("{}")
                                .withStatus(200)
                ))
    }

    internal fun `stub response that wont map`(antall: Int, fraSekvensnummer: Long) {
        skattApiHendelseMock.stubFor(WireMock.get(WireMock.urlPathEqualTo(HENDELSE_PATH))
                .withQueryParams(
                        mapOf(
                                ANTALL_KEY to WireMock.equalTo((antall.toString())),
                                FRA_SEKVENSNUMMER_KEY to WireMock.equalTo((fraSekvensnummer.toString()))
                        )
                )
                .willReturn(
                        WireMock.aResponse()
                                .withBody("[")
                                .withStatus(200)
                ))
    }



    internal fun stop() {
        skattApiHendelseMock.stop()
    }
}