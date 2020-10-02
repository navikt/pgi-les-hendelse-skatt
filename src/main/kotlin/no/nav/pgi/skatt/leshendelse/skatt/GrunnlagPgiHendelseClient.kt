package no.nav.pgi.skatt.leshendelse.skatt

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.pgi.skatt.leshendelse.getVal
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers.ofString

internal const val HENDELSE_HOST_ENV_KEY = "grunnlag-pgi-hendelse-host"
internal const val HENDELSE_PATH = "/api/skatteoppgjoer/ekstern/grunnlag-pgi/hendelse"

internal class GrunnlagPgiHendelseClient(env: Map<String, String> = System.getenv()) {
    private val host: String = env.getVal(HENDELSE_HOST_ENV_KEY)
    val objectMapper = ObjectMapper().registerModule(KotlinModule())
    private val skattClient = SkattClient(env)

    fun send(antall: Int, fraSekvensnummer: Long): Hendelser {
        val response = skattClient.send(skattClient.createGetRequest(host + HENDELSE_PATH, queryParameters(antall, fraSekvensnummer)), ofString())
        return mapResponse(response)
    }

    private fun queryParameters(antall: Int, fraSekvensnummer: Long) = mapOf("antall" to antall, "fraSekvensnummer" to fraSekvensnummer)

    private fun mapResponse(response: HttpResponse<String>): Hendelser {
        val hendelser = when (response.statusCode()) {
            200 -> readValue(response.body())
            else -> throw GrunnlagPgiHendelseClientCallException(response.body())
        }
        hendelser.validate()
        return hendelser
    }


    private fun readValue(body: String): Hendelser {
        return try {
            objectMapper.readValue(body)
        } catch (e: Exception) {
            throw GrunnlagPgiHendelseClientObjectMapperException(e.toString())
        }
    }
}

internal class GrunnlagPgiHendelseClientCallException(message: String) : Exception(message)
internal class GrunnlagPgiHendelseClientObjectMapperException(message: String) : Exception(message)



