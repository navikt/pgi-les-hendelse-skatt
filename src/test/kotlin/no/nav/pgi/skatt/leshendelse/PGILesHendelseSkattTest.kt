package no.nav.pgi.skatt.leshendelse

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.*
import no.nav.pgi.skatt.leshendelse.hendelserskatt.Hendelser
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import org.json.JSONObject
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers.ofString


private const val HOST = "http://localhost"

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PGILesHendelseSkattTest {

    private val client = HttpClient.newHttpClient()
    private val kafkaTestEnvironment = KafkaTestEnvironment()
    private val kafkaConfig = KafkaConfig(kafkaTestEnvironment.testConfiguration())
    private val application = createApplication(kafkaConfig = kafkaConfig)
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

    @Test
    @Order(1)
    fun `get first sekvensnummer empty, call Skatteetaten REST service`() {
        val sekvensnummerConsumer = SekvensnummerConsumer(kafkaConfig, TopicPartition(KafkaConfig.NEXT_SEKVENSNUMMER_TOPIC, 0))
        var sekvensnummer = sekvensnummerConsumer.getLastSekvensnummer()
        if (sekvensnummer == null) {
            val httpRequest = createGetRequest(SKATT_API_PORT, SKATT_FIRST_HENDELSE_URL)
            val response = client.send(httpRequest, ofString())
            sekvensnummer = JSONObject(response.body()).getInt("sekvensnummer").toString()
        }

        assertEquals("1", sekvensnummer)
        sekvensnummerConsumer.close()
    }

    @Test
    fun `get first sekvensnummer skatt`() {
        val httpRequest = createGetRequest(SKATT_API_PORT, SKATT_FIRST_HENDELSE_URL)
        val response = client.send(httpRequest, ofString())

        assertEquals(HttpStatusCode.OK.value, response.statusCode())
        assertEquals(1, JSONObject(response.body()).getInt("sekvensnummer"))
    }


    @Test
    fun `get last sekvensnummer from topic`() {
        val sekvensnummerConsumer = SekvensnummerConsumer(kafkaConfig, TopicPartition(KafkaConfig.NEXT_SEKVENSNUMMER_TOPIC, 0))

        val lastSekvensnummer = "5"
        addListOfSekvensnummerToTopic(listOf("1", "2", "3", "4", lastSekvensnummer))

        assertEquals(lastSekvensnummer, sekvensnummerConsumer.getLastSekvensnummer())
        sekvensnummerConsumer.close()

    }

    @Test
    fun `write sekvensnummer to topic`() {
        val sekvensnummerProducer = SekvensnummerProducer(kafkaConfig)
        val sekvensnummerConsumer = SekvensnummerConsumer(kafkaConfig, TopicPartition(KafkaConfig.NEXT_SEKVENSNUMMER_TOPIC, 0))
        sekvensnummerProducer.writeSekvensnummer("1234")

        assertEquals("1234", sekvensnummerConsumer.getLastSekvensnummer())
        sekvensnummerConsumer.close()
    }


    @Test
    fun `get hendelser from skatt`() {
        val httpRequest = createGetRequest(HENDELSE_PORT, HENDELSE_URL)
        val response = client.send(httpRequest, ofString())

        assertEquals(HttpStatusCode.OK.value, response.statusCode())

        val hendelser = ObjectMapper().readValue(response.body(), Hendelser::class.java)
        assertTrue(hendelser.hendelser.size == 5)
    }

    @Test
    fun `write pgi hendelse to topic`() {
        val record = ProducerRecord(KafkaConfig.PGI_HENDELSE_TOPIC, "", "Hendelse")
        kafkaConfig.hendelseProducer().send(record)
        kafkaConfig.hendelseProducer().flush()
        assertEquals("Hendelse", kafkaTestEnvironment.getFirstRecordOnTopic().value())
    }

    private fun addListOfSekvensnummerToTopic(sekvensnummerList: List<String>) {
        sekvensnummerList.indices.forEach { i -> addSekvensnummerToTopic(sekvensnummerList[i]) }
    }

    private fun addSekvensnummerToTopic(sekvensnummer: String) {
        val sekvensnummerProducer = SekvensnummerProducer(kafkaConfig)
        sekvensnummerProducer.writeSekvensnummer(sekvensnummer)
    }

    private fun createGetRequest(port: Int, url: String) = HttpRequest.newBuilder()
            .uri(URI.create("$HOST:$port$url"))
            .GET()
            .build()
}
