package no.nav.pgi.skatt.leshendelse.skatt

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.pensjon.samhandling.env.getVal
import no.nav.pgi.skatt.leshendelse.HentSekvensnummer
import org.slf4j.LoggerFactory
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers.ofString

internal const val FIRST_SEKVENSNUMMER_HOST_ENV_KEY = "GRUNNLAG_PGI_FIRST_SEKVENSNUMMER_HOST"
internal const val FIRST_SEKVENSNUMMER_PATH_ENV_KEY = "SKATT_HENDELSE_START_PATH"
private val LOG = LoggerFactory.getLogger(FirstSekvensnummerClient::class.java)

internal class FirstSekvensnummerClient(env: Map<String, String> = System.getenv()) : SkattClient(env) {
    private val host = env.getVal(FIRST_SEKVENSNUMMER_HOST_ENV_KEY)
    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
    private val url = """$host${env.getVal(FIRST_SEKVENSNUMMER_PATH_ENV_KEY)}"""

    fun getSekvensnummer(fra: HentSekvensnummer = HentSekvensnummer.FørsteMulige): Long {
        val params = when (fra) {
            is HentSekvensnummer.FraDato -> mapOf("dato" to fra.date.toString())
            HentSekvensnummer.FørsteMulige -> emptyMap()
        }

        val response = send(createGetRequest(url, params), ofString())
        return when (response.statusCode()) {
            200 -> mapResponse(response.body()).also { LOG.info("Received $it as first sekvensnummer from skatt") }
            else -> throw FirstSekvensnummerClientCallException(response).also { LOG.error(it.message) }
        }
    }

    private fun mapResponse(body: String) =
        try {
            objectMapper.readValue(body, SekvensnummerDto::class.java).sekvensnummer
        } catch (e: Exception) {
            throw FirstSekvensnummerClientMappingException(e.toString())
        }
}

internal data class SekvensnummerDto(@JsonProperty(value = "sekvensnummer", required = true) val sekvensnummer: Long)
internal class FirstSekvensnummerClientMappingException(message: String) : Exception(message)
internal class FirstSekvensnummerClientCallException(response: HttpResponse<String>) :
    Exception("Feil ved henting første sekvensnummer: Status: ${response.statusCode()} , Body: ${response.body()}")