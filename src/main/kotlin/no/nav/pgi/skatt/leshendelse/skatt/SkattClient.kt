package no.nav.pgi.skatt.leshendelse.skatt

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse


class SkattClient(private val httpClient: HttpClient = HttpClient.newHttpClient()) {
    internal fun <T> send(httpRequest: HttpRequest, responseBodyHandler: HttpResponse.BodyHandler<T>): HttpResponse<T> =
            httpClient.send(httpRequest, responseBodyHandler)
}

internal fun createGetRequest(url: String) = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .GET()
        .build()