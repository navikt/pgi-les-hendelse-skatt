package no.nav.pgi.skatt.leshendelse.skatt

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.pensjon.samhandling.env.getVal
import org.slf4j.LoggerFactory
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers.ofString

internal const val HENDELSE_HOST_ENV_KEY = "GRUNNLAG_PGI_HENDELSE_HOST"
internal const val HENDELSE_PATH = "/api/formueinntekt/pensjonsgivendeinntektforfolketrygden/hendelse"
private val LOG = LoggerFactory.getLogger(HendelseClient::class.java)

internal class HendelseClient(env: Map<String, String>) {
    private val host: String = env.getVal(HENDELSE_HOST_ENV_KEY)
    private val objectMapper = ObjectMapper().registerModule(KotlinModule())
    private val skattClient = SkattClient(env)

    fun getHendelserSkatt(antall: Int, fraSekvensnummer: Long): HendelserDto {
        val response = skattClient.send(skattClient.createGetRequest(host + HENDELSE_PATH, createQueryParameters(antall, fraSekvensnummer)), ofString())
        return when (response.statusCode()) {
            200 -> mapResponse(response.body()).also { LOG.info("Polled ${it.hendelser.size} hendelser from skatt") }
            else -> throw HendelseClientCallException(response).also { LOG.error(it.message) }
        }
    }

    private fun mapResponse(body: String): HendelserDto =
            try {
                objectMapper.readValue(body)
            } catch (e: Exception) {
                throw HendelseClientObjectMapperException(e.toString()).also { LOG.error(it.message) }
            }

    private fun createQueryParameters(antall: Int, fraSekvensnummer: Long) = mapOf("antall" to antall, "fraSekvensnummer" to fraSekvensnummer)
}

internal class HendelseClientCallException(response: HttpResponse<String>) : Exception("Feil ved henting av hendelse mot skatt: Status: ${response.statusCode()} , Body: ${response.body()}")
internal class HendelseClientObjectMapperException(message: String) : Exception(message)
