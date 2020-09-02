package no.nav.pgi.skatt.leshendelse

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.*
import no.nav.pgi.skatt.leshendelse.hendelserskatt.Hendelser
import org.json.JSONObject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers.ofString


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal object PGILesHendelseSkattTest {

    private const val APPLICATION_PORT = 8080
    private const val HOST = "http://localhost"
    private val application = createApplication()
    private val client = HttpClient.newHttpClient()
    private val kafkaTestEnvironment = KafkaTestEnvironment()

    private val sekvensnummerMock = FirstSekvensnummerMock()
    private val hendelseMock = HendelseMock()

    @BeforeAll
    internal fun init() {
        sekvensnummerMock.`stub first sekvensnummer endepunkt skatt`()
        hendelseMock.`stub hendelse endepunkt skatt`()
        application.start()
    }

    @AfterAll
    internal fun teardown() {
        application.stop(100, 100)
        kafkaTestEnvironment.tearDown()
        sekvensnummerMock.stop()
        hendelseMock.stop()
    }

    //region Tests
    @Test
    fun TestMockhentSekvensnummerFraTopic() {
        val httpRequest = createGetRequest(HOST, SKATT_API_PORT, SKATT_FIRST_HENDELSE_URL)
        val response = client.send(httpRequest, ofString())

        assertEquals(HttpStatusCode.OK.value, response.statusCode())
        assertEquals(1, JSONObject(response.body()).getInt("sekvensnummer"))
    }

    @Test
    fun TestMockHendelser() {
        val httpRequest = createGetRequest(HOST, HENDELSE_PORT, HENDELSE_URL)
        val response = client.send(httpRequest, ofString())

        println(response.body())

        assertEquals(HttpStatusCode.OK.value, response.statusCode())

        val hendelser = ObjectMapper().readValue(response.body(), Hendelser::class.java)
        assertTrue(hendelser.hendelser.size == 5)
    }

    @Test
    fun isAlive() {
        val response = client.send(createGetRequest(HOST, APPLICATION_PORT, IS_ALIVE_PATH), ofString())
        assertEquals(HttpStatusCode.OK.value, response.statusCode())
    }

    @Test
    fun isReady() {
        val response = client.send(createGetRequest(HOST, APPLICATION_PORT, IS_READY_PATH), ofString())
        assertEquals(HttpStatusCode.OK.value, response.statusCode())
    }
    //endregion

    private fun createGetRequest(host: String, port: Int, url: String) = HttpRequest.newBuilder()
            .uri(URI.create("$host:$port$url"))
            .GET()
            .build()
}