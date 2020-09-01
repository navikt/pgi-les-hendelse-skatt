package no.nav.pgi.skatt.leshendelse

import io.ktor.http.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers.ofString

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal object PgiLesHendelseSkatt {

    private const val SERVER_PORT = 8080
    private const val HOST = "http://localhost:$SERVER_PORT"
    private val application = createApplication()
    private val client = HttpClient.newHttpClient()

    @BeforeAll
    fun init() {
        application.start()
    }

    @AfterAll
    fun teardown() {
        application.stop(100, 100)
    }

    @Test
    fun isAlive() {
        val response = client.send(createGetRequest(IS_ALIVE_PATH), ofString())
        assertEquals(HttpStatusCode.OK.value, response.statusCode())
    }

    @Test
    fun isReady() {
        val response = client.send(createGetRequest(IS_READY_PATH), ofString())
        assertEquals(HttpStatusCode.OK.value, response.statusCode())
    }

    private fun createGetRequest(path: String): HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create("$HOST/$path"))
            .GET()
            .build()

}