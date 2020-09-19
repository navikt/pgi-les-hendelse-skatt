package no.nav.pgi.skatt.leshendelse

import io.ktor.http.*
import no.nav.pgi.skatt.leshendelse.skatt.GrunnlagPgiHendelseClient
import no.nav.pgi.skatt.leshendelse.skatt.Hendelse
import no.nav.pgi.skatt.leshendelse.skatt.SkattClient
import no.nav.pgi.skatt.leshendelse.skatt.createGetRequest
import org.apache.kafka.common.TopicPartition
import org.json.JSONObject
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.net.http.HttpResponse.BodyHandlers.ofString


@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PGILesHendelseSkattTest {
    private val grunnlagPgiHendelseClient = GrunnlagPgiHendelseClient(HENDELSE_URL)
    private val client = SkattClient()
    private val kafkaTestEnvironment = KafkaTestEnvironment()
    private val kafkaConfig = KafkaConfig(kafkaTestEnvironment.testConfiguration())
    private val sekvensnummerProducer = SekvensnummerProducer(kafkaConfig)
    private val sekvensnummerConsumer = SekvensnummerConsumer(kafkaConfig, TopicPartition(KafkaConfig.NEXT_SEKVENSNUMMER_TOPIC, 0))
    private val hendelseProducer = HendelseProducer(kafkaConfig)
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
        sekvensnummerConsumer.close()
    }

    @Test
    @Order(1)
    fun `get first sekvensnummer empty, call Skatteetaten REST service`() {
        var sekvensnummer = sekvensnummerConsumer.getLastSekvensnummer()
        if (sekvensnummer == null) {
            val httpRequest = createGetRequest(FIRST_SEKVENSNUMMER_URL)
            val response = client.send(httpRequest, ofString())
            sekvensnummer = JSONObject(response.body()).getInt("sekvensnummer").toString()
        }

        assertEquals("1", sekvensnummer)
    }

    @Test
    fun `get first sekvensnummer skatt`() {
        val httpRequest = createGetRequest(FIRST_SEKVENSNUMMER_URL)
        val response = client.send(httpRequest, ofString())

        assertEquals(HttpStatusCode.OK.value, response.statusCode())
        assertEquals(1, JSONObject(response.body()).getInt("sekvensnummer"))
    }


    @Test
    fun `get last sekvensnummer from topic`() {
        val lastSekvensnummer = "5"
        addListOfSekvensnummerToTopic(listOf("1", "2", "3", "4", lastSekvensnummer))

        assertEquals(lastSekvensnummer, sekvensnummerConsumer.getLastSekvensnummer())
    }

    @Test
    fun `write sekvensnummer to topic`() {
        sekvensnummerProducer.writeSekvensnummer("1234")
        assertEquals("1234", sekvensnummerConsumer.getLastSekvensnummer())
    }


    @Test
    fun `get hendelser from skatt`() {
        val hendelser = grunnlagPgiHendelseClient.send(1000,1L)
        assertTrue(hendelser.size() == 5)
    }

    @Test
    fun `write pgi hendelse to topic`() {
        val hendelse = Hendelse("123456", "12345", 1L)
        hendelseProducer.writeHendelse(hendelse)
        assertEquals(hendelse.toString(), kafkaTestEnvironment.getFirstRecordOnTopic().value())
    }

    private fun addListOfSekvensnummerToTopic(sekvensnummerList: List<String>) {
        sekvensnummerList.indices.forEach { i -> sekvensnummerProducer.writeSekvensnummer(sekvensnummerList[i]) }
    }
}
