package no.nav.pgi.skatt.leshendelse

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse


internal const val HENDELSE_PORT = 8085
internal const val HENDELSE_URL = "/api/skatteoppgjoer/ekstern/grunnlag-pgi/hendelse/start"

internal class HendelseMock {

    private val skattApiMock = WireMockServer(HENDELSE_PORT)

    init {
        skattApiMock.start()
    }

    // region Stubs
    internal fun `stub hendelse endepunkt skatt`() {
        skattApiMock.stubFor(WireMock.get(WireMock.urlPathEqualTo(HENDELSE_URL)).willReturn(
                aResponse()
                        .withBodyFile("Hendelser.json")
                        .withStatus(200)
        ))
    }
    // endregion

    internal fun stop() {
        skattApiMock.stop()
    }
}


/*private fun createHendelser2(): String {
    return newObjectMapper().writeValueAsString(
            Hendelser(
                    ArrayList<Hendelse>().apply {
                        add(Hendelse("1", "09048000875", "2018"))
                        add(Hendelse(12, "20125001158", "2018"))
                        add(Hendelse(23, "02043700564", "2018"))
                        add(Hendelse(34, "17014200150", "2018"))
                        add(Hendelse(45, "17055401993", "2018"))
                    }
            )
    )
}*/

/*
    data class Hendelser(var hendelser: ArrayList<Hendelse> = ArrayList())
    data class Hendelse(var sekvensnummer: Int = 0, var identifikator: String = "", var gjelderPeriode: String = "")
 */



