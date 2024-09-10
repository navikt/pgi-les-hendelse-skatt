package no.nav.pgi.skatt.leshendelse.skatt

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import net.logstash.logback.marker.Markers
import no.nav.pgi.skatt.leshendelse.Counters
import no.nav.pgi.skatt.leshendelse.util.getVal
import no.nav.pgi.skatt.leshendelse.util.maskFnr
import org.slf4j.LoggerFactory
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers.ofString

internal const val HENDELSE_HOST_ENV_KEY = "GRUNNLAG_PGI_HENDELSE_HOST"
internal const val HENDELSE_PATH_ENV_KEY = "SKATT_HENDELSE_PATH"
private val LOG = LoggerFactory.getLogger(HendelseClient::class.java)

internal class HendelseClient(
    private val counters: Counters,
    env: Map<String, String>
) : SkattClient(env) {
    private val host: String = env.getVal(HENDELSE_HOST_ENV_KEY)
    private val objectMapper = ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .registerModule(KotlinModule.Builder().build())
    private val url: String = """$host${env.getVal(HENDELSE_PATH_ENV_KEY)}"""

    fun getHendelserSkatt(antall: Int, fraSekvensnummer: Long): List<HendelseDto> {
        val response = send(createGetRequest(url, createQueryParameters(antall, fraSekvensnummer)), ofString())
        return when (response.statusCode()) {
            200 -> mapResponse(response.body()).also { logPolledHendelser(it) }
            else -> throw HendelseClientCallException(response)
        }
    }

    private fun mapResponse(body: String): List<HendelseDto> =
        try {
            objectMapper.readValue(body, HendelserDtoWrapper::class.java).hendelser
        } catch (e: Exception) {
            throw HendelseClientObjectMapperException(e.toString()).also { LOG.error(it.message) }
        }

    private fun createQueryParameters(antall: Int, fraSekvensnummer: Long) =
        mapOf("antall" to antall, "fraSekvensnummer" to fraSekvensnummer)

    private fun logPolledHendelser(hendelser: List<HendelseDto>) {
        if (hendelser.isNotEmpty()) {
            LOG.info("Polled ${hendelser.size} hendelser from skatt. Containing sekvensnummer from ${hendelser.fistSekvensnummer()} to ${hendelser.lastSekvensnummer()}")
            hendelser.forEach { hendelse ->
                LOG.info(
                    Markers.append("sekvensnummer", hendelse.sekvensnummer.toString()),
                    "Lest hendelse: ${hendelse.mapToHendelse().toString().maskFnr()}"
                )
            }
        } else {
            LOG.info("Polled 0 hendelser from skatt.")
        }
        counters.incrementPolledFromSkatt(hendelser.size)
    }
}

internal class HendelseClientCallException(response: HttpResponse<String>) :
    Exception("Feil ved henting av hendelse mot skatt: Url:${response.uri()} Status: ${response.statusCode()} , Body: ${response.body()}")

internal class HendelseClientObjectMapperException(message: String) : Exception(message)