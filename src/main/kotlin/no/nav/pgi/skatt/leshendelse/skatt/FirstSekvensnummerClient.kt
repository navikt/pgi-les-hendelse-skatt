package no.nav.pgi.skatt.leshendelse.skatt

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.pensjon.samhandling.env.getVal
import org.slf4j.LoggerFactory
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers.ofString

internal const val FIRST_SEKVENSNUMMER_HOST_ENV_KEY = "GRUNNLAG_PGI_FIRST_SEKVENSNUMMER_HOST"
internal const val FIRST_SEKVENSNUMMER_PATH = "/api/formueinntekt/pensjonsgivendeinntektforfolketrygden/hendelse/start"
private val LOGGER = LoggerFactory.getLogger(FirstSekvensnummerClient::class.java)

internal class FirstSekvensnummerClient(env: Map<String, String> = System.getenv()) {
    private val host = env.getVal(FIRST_SEKVENSNUMMER_HOST_ENV_KEY)
    private val objectMapper = ObjectMapper().registerModule(KotlinModule())
    private val skattClient = SkattClient(env)

    fun getFirstSekvensnummer(): Long {
        val response = skattClient.send(skattClient.createGetRequest(host + FIRST_SEKVENSNUMMER_PATH), ofString())
        return when (response.statusCode()) {
            200 -> mapResponse(response.body()).also{ LOGGER.info("Received $it as first sekvensnummer from skatt")}
            else -> throw FirstSekvensnummerClientCallException(response).also { LOGGER.error(it.message) }
        }
    }

    private fun mapResponse(body: String) =
            try {
                objectMapper.readValue(body, Sekvensnummer::class.java).sekvensnummer
            } catch (e: Exception) {
                throw FirstSekvensnummerClientMappingException(e.toString())
            }
}

internal data class Sekvensnummer(@JsonProperty(value = "sekvensnummer", required = true) val sekvensnummer: Long)
internal class FirstSekvensnummerClientMappingException(message: String) : Exception(message)
internal class FirstSekvensnummerClientCallException(response: HttpResponse<String>) : Exception("Feil ved henting første sekvensnummer: Status: ${response.statusCode()} , Body: ${response.body()}")