package no.nav.pgi.skatt.leshendelse

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.*
import no.nav.pgi.skatt.leshendelse.hendelserskatt.Hendelser
import org.apache.kafka.clients.consumer.ConsumerRecord
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
import java.time.Duration


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal object PGILesHendelseSkattTest {

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

    @Test
    fun `Get first sekvensnummer skatt`() {
        val httpRequest = createGetRequest(SKATT_API_PORT, SKATT_FIRST_HENDELSE_URL)
        val response = client.send(httpRequest, ofString())

        assertEquals(HttpStatusCode.OK.value, response.statusCode())
        assertEquals(1, JSONObject(response.body()).getInt("sekvensnummer"))
    }

    @Test
    fun `Get sekvensnummer from topic`() {
        val kafkaConfig = KafkaConfig(kafkaTestEnvironment.kafkaEnvVariables())
        addSekvensnummerToTopic(kafkaConfig, "1111")
        addSekvensnummerToTopic(kafkaConfig, "2222")
        addSekvensnummerToTopic(kafkaConfig, "3333")

        val sekvensnummerConsumer = kafkaConfig.nextSekvensnummerConsumer()
        sekvensnummerConsumer.subscribe(listOf(KafkaConfig.NEXT_SEKVENSNUMMER_TOPIC))

        //TODO forsett her Pål
        //sekvensnummerConsumer.listTopics()[KafkaConfig.NEXT_SEKVENSNUMMER_TOPIC]?.forEach {
        //    sekvensnummerConsumer.seekToEnd(listOf(TopicPartition(KafkaConfig.NEXT_SEKVENSNUMMER_TOPIC, it.partition())))
        //}

        val records: List<ConsumerRecord<String, String>> = sekvensnummerConsumer.poll(Duration.ofSeconds(4)).records(KafkaConfig.NEXT_SEKVENSNUMMER_TOPIC).toList()


    }

    @Test
    fun `Get sekvensnummer from topic when there is non`() {
        val kafkaConfig = KafkaConfig(kafkaTestEnvironment.kafkaEnvVariables())

        val sekvensnummerConsumer = kafkaConfig.nextSekvensnummerConsumer()
        sekvensnummerConsumer.subscribe(listOf(KafkaConfig.NEXT_SEKVENSNUMMER_TOPIC))
        sekvensnummerConsumer.poll(Duration.ofSeconds(4)).records(KafkaConfig.NEXT_SEKVENSNUMMER_TOPIC).toList()

    }

    @Test
    fun `Get hendelser from skatt`() {
        val httpRequest = createGetRequest(HENDELSE_PORT, HENDELSE_URL)
        val response = client.send(httpRequest, ofString())

        println(response.body())

        assertEquals(HttpStatusCode.OK.value, response.statusCode())

        val hendelser = ObjectMapper().readValue(response.body(), Hendelser::class.java)
        assertTrue(hendelser.hendelser.size == 5)
    }

    @Test
    fun `Write pgi hendelse to topic`() {
        val kafkaConfig = KafkaConfig(kafkaTestEnvironment.kafkaEnvVariables())

        val record = ProducerRecord(KafkaConfig.PGI_HENDELSE_TOPIC, "", "Hendelse")
        kafkaConfig.hendelseProducer().send(record) { metadata, exception ->
            println(if (metadata == null) exception.toString() else "Value size: " + metadata.serializedValueSize())
        }
        kafkaConfig.hendelseProducer().flush()

        assertEquals("Hendelse", kafkaTestEnvironment.getFirstRecordOnTopic().value())

    }

    @Test
    fun `Write sekvensnummer to topic`() {
        val kafkaConfig = KafkaConfig(kafkaTestEnvironment.kafkaEnvVariables())
        addSekvensnummerToTopic(kafkaConfig, "134234234")

        kafkaConfig.nextSekvensnummerProducer().flush()
    }

    private fun addSekvensnummerToTopic(kafkaConfig: KafkaConfig, sekvensnummer: String) {
        val record = ProducerRecord(KafkaConfig.NEXT_SEKVENSNUMMER_TOPIC, "", sekvensnummer)
        kafkaConfig.nextSekvensnummerProducer().send(record).get()
    }

    private fun createGetRequest(port: Int, url: String) = HttpRequest.newBuilder()
            .uri(URI.create("$HOST:$port$url"))
            .GET()
            .build()
}