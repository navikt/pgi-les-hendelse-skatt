package no.nav.pgi.skatt.leshendelse

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.*
import no.nav.pgi.skatt.leshendelse.hendelserskatt.Hendelser
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
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
import java.time.Duration.ofSeconds


private const val HOST = "http://localhost"

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
    fun `get first sekvensnummer skatt`() {
        val httpRequest = createGetRequest(SKATT_API_PORT, SKATT_FIRST_HENDELSE_URL)
        val response = client.send(httpRequest, ofString())

        assertEquals(HttpStatusCode.OK.value, response.statusCode())
        assertEquals(1, JSONObject(response.body()).getInt("sekvensnummer"))
    }

    @Test
    fun `get last sekvensnummer from topic`() {
        val sekvensnummerConsumer = SekvensnummerConsumer(kafkaConfig, TopicPartition(KafkaConfig.NEXT_SEKVENSNUMMER_TOPIC, 0))

        val lastSekvensnummer = "4444"
        addListOfSekvensnummerToTopic(listOf("1111", "2222", "3333", lastSekvensnummer))
        assertEquals(lastSekvensnummer, sekvensnummerConsumer.getLastSekvensnummer())
    }

    @Test
    fun `write sekvensnummer to topic`() {
        val sekvensnummerProducer = SekvensnummerProducer(kafkaConfig)
        val sekvensnummerConsumer = SekvensnummerConsumer(kafkaConfig, TopicPartition(KafkaConfig.NEXT_SEKVENSNUMMER_TOPIC, 0))
        sekvensnummerProducer.writeSekvensnummer("1234")
        assertEquals("1234", sekvensnummerConsumer.getLastSekvensnummer())
    }

    @Test
    fun `get sekvensnummer from topic when there is none`() {
        val sekvensnummerConsumer = kafkaConfig.nextSekvensnummerConsumer()
        sekvensnummerConsumer.subscribe(listOf(KafkaConfig.NEXT_SEKVENSNUMMER_TOPIC))
        sekvensnummerConsumer.poll(ofSeconds(4)).records(KafkaConfig.NEXT_SEKVENSNUMMER_TOPIC).toList()
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
        val record = ProducerRecord(KafkaConfig.NEXT_SEKVENSNUMMER_TOPIC, "", sekvensnummer)
        kafkaConfig.nextSekvensnummerProducer().send(record).get()
    }

    private fun createGetRequest(port: Int, url: String) = HttpRequest.newBuilder()
            .uri(URI.create("$HOST:$port$url"))
            .GET()
            .build()
}
