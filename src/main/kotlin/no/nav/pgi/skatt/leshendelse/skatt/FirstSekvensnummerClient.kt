package no.nav.pgi.skatt.leshendelse.skatt

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.pgi.skatt.leshendelse.getVal
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers.ofString

internal const val FIRST_SEKVENSNUMMER_HOST_ENV_KEY = "grunnlag-pgi-first-sekvensnummer-host-key"
internal const val FIRST_SEKVENSNUMMER_PATH = "/api/skatteoppgjoer/ekstern/grunnlag-pgi/hendelse/start"

internal class FirstSekvensnummerClient(env: Map<String, String> = System.getenv()) {
    private val host = env.getVal(FIRST_SEKVENSNUMMER_HOST_ENV_KEY)
    private val objectMapper = ObjectMapper().registerModule(KotlinModule())
    private val skattClient = SkattClient(env)

    fun send(): Long {
        val response = skattClient.send(skattClient.createGetRequest(host + FIRST_SEKVENSNUMMER_PATH), ofString())
        return mapResponse(response)
    }

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