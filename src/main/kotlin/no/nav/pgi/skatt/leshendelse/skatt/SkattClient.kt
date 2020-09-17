package no.nav.pgi.skatt.leshendelse.skatt

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers.ofString

class SkattClient(private val httpClient: HttpClient = HttpClient.newHttpClient()) {

    internal fun createGetRequest(url: String) = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build()

    internal fun send(httpRequest: HttpRequest) = httpClient.send(httpRequest, ofString())

}