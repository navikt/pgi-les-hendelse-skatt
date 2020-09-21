package no.nav.pgi.skatt.leshendelse.skatt

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.pgi.skatt.leshendelse.getVal
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers.ofString

private const val FIRST_SEKVENSNUMMER_HOST_KEY = "grunnlag-pgi-first-sekvensnummer-host-key"
internal const val FIRST_SEKVENSNUMMER_PATH = "/api/skatteoppgjoer/ekstern/grunnlag-pgi/hendelse/start"

internal class FirstSekvensnummerClient(private val host: String = System.getenv().getVal(FIRST_SEKVENSNUMMER_HOST_KEY)) {
    private val objectMapper = ObjectMapper().registerModule(KotlinModule())
    private val SkattClient = SkattClient()

    fun send(): Long {
        val response = SkattClient.send(createGetRequest(host + FIRST_SEKVENSNUMMER_PATH), ofString())
        return mapResponse(response)
    }

    //TODO hente sekvensnummer (json)
    //TODO håndtere feilmelding

    private fun mapResponse(response: HttpResponse<String>): Long {
        return readValue(response.body())
    }

    private fun readValue(body: String): Long {

        return try {
            objectMapper.readValue(body, Sekvensnummer::class.java).sekvensnummer
        } catch (e: Exception) {
            throw FirstSekvensnummerException(e.toString())
        }
    }
}

internal data class Sekvensnummer(val sekvensnummer: Long)

internal class FirstSekvensnummerException(message: String) : Exception(message)

//https://api-st.sits.no/api/skatteoppgjoer/ekstern/grunnlag-pgi/hendelse/start?dato={YYYY-MM-DD}