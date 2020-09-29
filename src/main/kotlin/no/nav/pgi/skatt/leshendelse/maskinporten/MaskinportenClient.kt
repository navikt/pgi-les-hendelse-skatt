package no.nav.pgi.skatt.leshendelse.maskinporten

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.pgi.skatt.leshendelse.getVal
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers.ofString
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers.ofString

internal const val MASKINPORTEN_CREATE_TOKEN_PATH = "/123/"
internal const val MASKINPORTEN_CREATE_TOKEN_HOST_ENV_KEY = "maskinporten-host"

internal const val GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer"
internal const val CONTENT_TYPE = "application/x-www-form-urlencoded"

internal class MaskinportenClient(env: Map<String, String> = System.getenv()) {
    private val host: String = env.getVal(MASKINPORTEN_CREATE_TOKEN_HOST_ENV_KEY)
    private val httpClient: HttpClient = HttpClient.newHttpClient()
    private val grantToken: MaskinportenGrantToken = MaskinportenGrantToken(env)
    //private val maskinportenJWKSValidation: MaskinportenJWKSValidation = MaskinportenJWKSValidation()

    private val objectMapper = ObjectMapper().registerModule(KotlinModule())
    private val tokenCache: TokenCache = TokenCache()

    internal fun getToken(): String {
        return if (tokenCache.isValidToken()) {
            tokenCache.getToken()
        } else {
            tokenCache.setToken(getTokenFromMaskinporten())
            tokenCache.getToken()
        }
    }

    private fun getTokenFromMaskinporten(): String {
        val response = httpClient.send(createTokenRequest(), ofString())
        if (response.statusCode() == 200) {
            val response = mapToMaskinportenResponseBody(response.body())
            //maskinportenJWKSValidation.validateToken(response.access_token)
            return response.access_token
        }
        throw MaskinportenGrantException(response)
    }

    private fun createTokenRequest() = HttpRequest.newBuilder()
            .uri(URI.create(host + MASKINPORTEN_CREATE_TOKEN_PATH))
            .header("Content-Type", CONTENT_TYPE)
            .POST(ofString(createRequestBody()))
            .build()

    private fun createRequestBody() =
            objectMapper.writeValueAsString(maskinportenRequestBody(assertion = grantToken.generateJwt()))


    private fun mapToMaskinportenResponseBody(responseBody: String): maskinportenResponseBody =
            try {
                objectMapper.readValue(responseBody)
            } catch (e: Exception) {
                throw MaskinportenObjectMapperException(e.toString())
            }

}

internal data class maskinportenRequestBody(val grant_type: String = GRANT_TYPE, val assertion: String)
internal data class maskinportenResponseBody(val access_token: String, val token_type: String?, val expires_in: Int?, val scope: String?)

internal class MaskinportenGrantException(response: HttpResponse<String>) : Exception("Feil ved henting av token: Status: ${response.statusCode()} , Body: ${response.body()}")
internal class MaskinportenObjectMapperException(message: String) : Exception("Feil ved deserialisering av response fra maskinporten: $message")

