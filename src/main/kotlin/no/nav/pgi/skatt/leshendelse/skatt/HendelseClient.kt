package no.nav.pgi.skatt.leshendelse.skatt

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.pensjon.samhandling.env.getVal
import org.slf4j.LoggerFactory
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers.ofString

internal const val HENDELSE_HOST_ENV_KEY = "grunnlag-pgi-hendelse-host"
internal const val HENDELSE_PATH = "/api/skatteoppgjoer/ekstern/grunnlag-pgi/hendelse"
private val LOGGER = LoggerFactory.getLogger(HendelseClient::class.java)

internal class HendelseClient(env: Map<String, String>) {
    private val host: String = env.getVal(HENDELSE_HOST_ENV_KEY)
    private val objectMapper = ObjectMapper().registerModule(KotlinModule())
    private val skattClient = SkattClient(env)

    fun getHendelserSkatt(antall: Int, fraSekvensnummer: Long): HendelserDto {
        LOGGER.info("""Starting to read $antall hendelser from sekvensnummer "$fraSekvensnummer"  """)
        val response = skattClient.send(skattClient.createGetRequest(host + HENDELSE_PATH, createQueryParameters(antall, fraSekvensnummer)), ofString())
        return when (response.statusCode()) {
            200 -> mapResponse(response.body()).also { LOGGER.info("Read ${it.hendelser.size} hendelser from skatt successfully.") }
            else -> throw HendelseClientCallException(response).also { LOGGER.error(it.message) }
        }
    }

    private fun mapResponse(body: String): HendelserDto =
            try {
                objectMapper.readValue(body)
            } catch (e: Exception) {
                throw HendelseClientObjectMapperException(e.toString()).also { LOGGER.error(it.message) }
            }

    private fun createQueryParameters(antall: Int, fraSekvensnummer: Long) = mapOf("antall" to antall, "fraSekvensnummer" to fraSekvensnummer)
}

internal class HendelseClientCallException(response: HttpResponse<String>) : Exception("Feil ved henting av hendelse mot skatt: Status: ${response.statusCode()} , Body: ${response.body()}")
internal class HendelseClientObjectMapperException(message: String) : Exception(message)
