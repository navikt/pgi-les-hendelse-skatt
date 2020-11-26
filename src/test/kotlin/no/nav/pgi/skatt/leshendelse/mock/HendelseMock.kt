package no.nav.pgi.skatt.leshendelse.mock

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import no.nav.pgi.skatt.leshendelse.ANTALL_HENDELSER
import no.nav.pgi.skatt.leshendelse.skatt.HENDELSE_PATH
import no.nav.pgi.skatt.leshendelse.skatt.HendelseDto
import no.nav.pgi.skatt.leshendelse.skatt.HendelserDto


private const val ANTALL_KEY = "antall"
private const val FRA_SEKVENSNUMMER_KEY = "fraSekvensnummer"

private const val HENDELSE_PORT = 8085
internal const val HENDELSE_MOCK_HOST = "http://localhost:$HENDELSE_PORT"


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

    internal fun `stub hendelse endpoint skatt`(fraSekvensnummer: Long) {
        var sekvensnummer = fraSekvensnummer
        mock.stubFor(WireMock.get(WireMock.urlPathEqualTo(HENDELSE_PATH))
                .willReturn(
                        aResponse()
                                .withBody(ObjectMapper()
                                        .registerModule(KotlinModule())
                                        .writeValueAsString(createHendelser(sekvensnummer, ANTALL_HENDELSER))
                                        .also {
                                            sekvensnummer += ANTALL_HENDELSER
                                        })
                                .withStatus(200)
                ))
    }

    internal fun `stub hendelse endpoint skatt`(fraSekvensnummer: Long, antall: Int): HendelserDto {
        val hendelser: HendelserDto = createHendelser(fraSekvensnummer, antall)
        mock.stubFor(WireMock.get(WireMock.urlPathEqualTo(HENDELSE_PATH))
                .withQueryParams(queryParams(fraSekvensnummer))
                .willReturn(
                        aResponse()
                                .withBody(ObjectMapper().registerModule(KotlinModule()).writeValueAsString(hendelser))
                                .withStatus(200)
                ))
        return hendelser
    }

    internal fun `stub hendelse endpoint response that wont map`(fraSekvensnummer: Long) {
        mock.stubFor(WireMock.get(WireMock.urlPathEqualTo(HENDELSE_PATH))
                .withQueryParams(queryParams(fraSekvensnummer))
                .willReturn(
                        aResponse()
                                .withBody("[")
                                .withStatus(200)
                ))
    }

    internal fun `stub hendelse endpoint response with masked data from skatt`(fraSekvensnummer: Long) {
        mock.stubFor(WireMock.get(WireMock.urlPathEqualTo(HENDELSE_PATH))
                .withQueryParams(queryParams(fraSekvensnummer))
                .willReturn(
                        aResponse()
                                .withBodyFile("Hendelser1To100.json")
                                .withStatus(200)
                ))
    }

    internal fun `stub hendelse endpoint first call`(fraSekvensnummer: Long, antall: Int): HendelserDto {
        val hendelser: HendelserDto = createHendelser(fraSekvensnummer, antall)
        mock.stubFor(WireMock.get(WireMock.urlPathEqualTo(HENDELSE_PATH))
                .withQueryParams(queryParams(fraSekvensnummer))
                .inScenario("Two calls to hendelse")
                .whenScenarioStateIs(STARTED)

                .willReturn(
                        aResponse()
                                .withBody(ObjectMapper().registerModule(KotlinModule()).writeValueAsString(hendelser))
                                .withStatus(200)
                )
                .willSetStateTo("First call completed"))
        return hendelser
    }

    internal fun `stub hendelse endpoint second call`(fraSekvensnummer: Long, antall: Int): HendelserDto {
        val hendelser: HendelserDto = createHendelser(fraSekvensnummer, antall)
        mock.stubFor(WireMock.get(WireMock.urlPathEqualTo(HENDELSE_PATH))
                .withQueryParams(queryParams(fraSekvensnummer))
                .inScenario("Two calls to hendelse")
                .whenScenarioStateIs("First call completed")
                .willReturn(
                        aResponse()
                                .withBody(ObjectMapper().registerModule(KotlinModule()).writeValueAsString(hendelser))
                                .withStatus(200)
                ).willSetStateTo("second call completed"))
        return hendelser
    }

    private fun queryParams(fraSekvensnummer: Long) =
            mapOf(
                    FRA_SEKVENSNUMMER_KEY to WireMock.equalTo((fraSekvensnummer.toString())),
                    ANTALL_KEY to WireMock.equalTo("$ANTALL_HENDELSER")
            )

    private fun createHendelser(startingSekvensnummer: Long, amount: Int): HendelserDto =
            HendelserDto(createHendelseList(startingSekvensnummer, amount))

    private fun createHendelseList(startingSekvensnummer: Long, amount: Int): List<HendelseDto> =
            (startingSekvensnummer until startingSekvensnummer + amount)
                    .toList()
                    .map { HendelseDto((11111111111 + it).toString(), "2020", it) }
}