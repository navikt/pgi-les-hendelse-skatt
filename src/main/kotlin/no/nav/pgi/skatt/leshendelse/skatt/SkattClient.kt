package no.nav.pgi.skatt.leshendelse.skatt


import no.nav.pensjon.samhandling.maskinporten.Maskinporten
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

open class SkattClient(env: Map<String, String> = System.getenv()) {
    private val maskinporten: Maskinporten = Maskinporten(env)
    private val httpClient: HttpClient = HttpClient.newHttpClient()
    internal fun <T> send(httpRequest: HttpRequest, responseBodyHandler: HttpResponse.BodyHandler<T>): HttpResponse<T> =
            httpClient.send(httpRequest, responseBodyHandler)

    internal fun createGetRequest(url: String, queryParameters: Map<String, Any> = emptyMap()) = HttpRequest.newBuilder()
            .uri(URI.create(url + queryParameters.createQueryString()))
            .GET()
            .setHeader("Authorization", "Bearer ${maskinporten.token}")
            .build()
}

internal fun Map<String, Any>.createQueryString() =
        if (isEmpty()) "" else keys.joinToString(prefix = "?", separator = "&") { "$it=${get(it).toString()}" }