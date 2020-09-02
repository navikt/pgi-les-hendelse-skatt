package no.nav.pgi.skatt.leshendelse

import io.ktor.http.*
import org.json.JSONObject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers.ofString
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertTrue


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal object PGILesHendelseSkattTest {

    private const val APPLICATION_PORT = 8080
    private const val HOST = "http://localhost:$APPLICATION_PORT"
    private val application = createApplication()
    private val client = HttpClient.newHttpClient()
    private val kafkaTestEnvironment = KafkaTestEnvironment()

    private val sekvensnummerMock = FirstSekvensnummerMock()
    private val hendelseMock = HendelseMock()

    @BeforeAll
    fun init() {
        sekvensnummerMock.`stub first sekvensnummer endepunkt skatt`()
        hendelseMock.`stub hendelse endepunkt skatt`()
        application.start()
    }

    @AfterAll
    fun teardown() {
        application.stop(100, 100)
        kafkaTestEnvironment.tearDown()
        sekvensnummerMock.stop()
        hendelseMock.stop()
    }

    @Test
    fun TestMockhentSekvensnummerFraTopic() {
        val response = client.send(createRequest("http://localhost:$SKATT_API_PORT$SKATT_FIRST_HENDELSE_URL"), ofString())
        assertEquals(HttpStatusCode.OK.value, response.statusCode())
        assertEquals(1, JSONObject(response.body()).getInt("sekvensnummer"))
    }

    @Test
    fun TestMockHendelser() {
        val response = client.send(createRequest("http://localhost:$HENDELSE_PORT$HENDELSE_URL"), ofString())
        assertEquals(HttpStatusCode.OK.value, response.statusCode())
        println(response.body())
        val hendelser = ObjectMapper().readValue(response.body(), Hendelser::class.java)
        assertTrue(hendelser.hendelser.size == 5)

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

    private fun createRequest(path: String): HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(path))
            .GET()
            .build()

}