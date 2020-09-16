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
import java.time.Duration.ofSeconds


private const val HOST = "http://localhost"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PGILesHendelseSkattTest {

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
    fun `get last sekvensnummer from populated topic`() {
        val kafkaConfig = KafkaConfig(kafkaTestEnvironment.kafkaEnvVariables())
        val sekvensnummerConsumer = kafkaConfig.nextSekvensnummerConsumer()

        val topicPartition = TopicPartition(KafkaConfig.NEXT_SEKVENSNUMMER_TOPIC, 0)
        sekvensnummerConsumer.assign(listOf(topicPartition))

        addSekvensnummerToTopic(kafkaConfig, "1111")
        addSekvensnummerToTopic(kafkaConfig, "2222")
        addSekvensnummerToTopic(kafkaConfig, "3333")
        addSekvensnummerToTopic(kafkaConfig, "3333")
        addSekvensnummerToTopic(kafkaConfig, "3333")

        val valueOfLastRecord = "4444"
        addSekvensnummerToTopic(kafkaConfig, valueOfLastRecord)


        val partitionSet = sekvensnummerConsumer.assignment()
        val usedPartition = partitionSet.elementAt(0)
        val endOffsets = sekvensnummerConsumer.endOffsets(partitionSet)
        val nextOffsetToBeCommitted: Long = endOffsets.entries.iterator().next().value.toLong()

        sekvensnummerConsumer.seek(usedPartition, nextOffsetToBeCommitted - 1)
        val records: List<ConsumerRecord<String, String>> = sekvensnummerConsumer.poll(ofSeconds(4)).records(topicPartition).toList()

        assertEquals(valueOfLastRecord, records.last().value())
    }


    @Test
    fun `Get sekvensnummer from topic when there is none`() {
        val kafkaConfig = KafkaConfig(kafkaTestEnvironment.kafkaEnvVariables())

        val sekvensnummerConsumer = kafkaConfig.nextSekvensnummerConsumer()
        sekvensnummerConsumer.subscribe(listOf(KafkaConfig.NEXT_SEKVENSNUMMER_TOPIC))
        sekvensnummerConsumer.poll(ofSeconds(4)).records(KafkaConfig.NEXT_SEKVENSNUMMER_TOPIC).toList()
        sekvensnummerConsumer.close()
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