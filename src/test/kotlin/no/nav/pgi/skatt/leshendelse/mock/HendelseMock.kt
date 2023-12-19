package no.nav.pgi.skatt.leshendelse.mock

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import no.nav.pgi.skatt.leshendelse.ANTALL_HENDELSER
import no.nav.pgi.skatt.leshendelse.skatt.HendelseDto
import no.nav.pgi.skatt.leshendelse.skatt.HendelserDtoWrapper


private const val ANTALL_KEY = "antall"
private const val FRA_SEKVENSNUMMER_KEY = "fraSekvensnummer"

private const val HENDELSE_PORT = 8085
internal const val HENDELSE_MOCK_HOST = "http://localhost:$HENDELSE_PORT"
internal const val HENDELSE_MOCK_PATH = "/api/formueinntekt/pensjonsgivendeinntektforfolketrygden/hendelser"


internal class HendelseMock {
    private val mock = WireMockServer(HENDELSE_PORT)

    init {
        mock.start()
    }

    internal fun reset() {
        mock.resetAll()
    }

    internal fun stop() {
        mock.stop()
    }

    internal fun `stub hendelse endpoint skatt`() {
        val hendelser: List<HendelseDto> = createHendelser(1, ANTALL_HENDELSER)
        mock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo(HENDELSE_MOCK_PATH))
                .willReturn(responseWithHendelser(hendelser))
        )
    }

    internal fun `stub hendelse endpoint skatt`(fraSekvensnummer: Long, antall: Int): List<HendelseDto> {
        val hendelser: List<HendelseDto> = createHendelser(fraSekvensnummer, antall)
        mock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo(HENDELSE_MOCK_PATH))
                .withQueryParams(queryParams(fraSekvensnummer))
                .willReturn(responseWithHendelser(hendelser))
        )
        return hendelser
    }

    internal fun `stub hendelse endpoint response that wont map`(fraSekvensnummer: Long) {
        mock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo(HENDELSE_MOCK_PATH))
                .withQueryParams(queryParams(fraSekvensnummer))
                .willReturn(
                    aResponse()
                        .withBody("[")
                        .withStatus(200)
                )
        )
    }

    internal fun `stub hendelse endpoint response with masked data from skatt`(fraSekvensnummer: Long) {
        mock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo(HENDELSE_MOCK_PATH))
                .withQueryParams(queryParams(fraSekvensnummer))
                .willReturn(
                    aResponse()
                        .withBodyFile("Hendelser1To100.json")
                        .withStatus(200)
                )
        )
    }

    internal fun `stub hendelse endpoint response with unknown fields from skatt`(fraSekvensnummer: Long) {
        mock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo(HENDELSE_MOCK_PATH))
                .withQueryParams(queryParams(fraSekvensnummer))
                .willReturn(
                    aResponse()
                        .withBody(
                            """
                                     {
                                     "hendelser": [
                                        {
                                          "sekvensnummer": 1,
                                          "identifikator": "12345678901",
                                          "gjelderPeriode": "2019",
                                          "unkwnown": "property"
                                        },
                                        {
                                          "sekvensnummer": 2,
                                          "identifikator": "12345678901",
                                          "gjelderPeriode": "2019",
                                          "unknown": null
                                        }
                                       ]
                                     }
                                 """.trimIndent()
                        )
                        .withStatus(200)
                )
        )
    }

    internal fun `stub hendelse endpoint first call`(fraSekvensnummer: Long, antall: Int): List<HendelseDto> {
        val hendelser: List<HendelseDto> = createHendelser(fraSekvensnummer, antall)
        mock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo(HENDELSE_MOCK_PATH))
                .withQueryParams(queryParams(fraSekvensnummer))
                .inScenario("Two calls to hendelse")
                .whenScenarioStateIs(STARTED)
                .willReturn(responseWithHendelser(hendelser))
                .willSetStateTo("First call completed")
        )
        return hendelser
    }

    internal fun `stub hendelse endpoint second call`(fraSekvensnummer: Long, antall: Int): List<HendelseDto> {
        val hendelser: List<HendelseDto> = createHendelser(fraSekvensnummer, antall)
        mock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo(HENDELSE_MOCK_PATH))
                .withQueryParams(queryParams(fraSekvensnummer))
                .inScenario("Two calls to hendelse")
                .whenScenarioStateIs("First call completed")
                .willReturn(responseWithHendelser(hendelser))
                .willSetStateTo("second call completed")
        )
        return hendelser
    }

    private fun responseWithHendelser(hendelser: List<HendelseDto>): ResponseDefinitionBuilder? {
        return aResponse()
            .withBody(
                ObjectMapper()
                    .registerModule(KotlinModule.Builder().build())
                    .writeValueAsString(HendelserDtoWrapper(hendelser))
            )
            .withStatus(200)
    }

    private fun queryParams(fraSekvensnummer: Long) =
        mapOf(
            FRA_SEKVENSNUMMER_KEY to WireMock.equalTo((fraSekvensnummer.toString())),
            ANTALL_KEY to WireMock.equalTo("$ANTALL_HENDELSER")
        )


    private fun createHendelser(startingSekvensnummer: Long, amount: Int): List<HendelseDto> =
        (startingSekvensnummer until startingSekvensnummer + amount)
            .toList()
            .map { HendelseDto((11111111111 + it).toString(), "2020", it) }
}