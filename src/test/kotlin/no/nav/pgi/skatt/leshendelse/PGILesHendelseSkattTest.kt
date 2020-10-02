package no.nav.pgi.skatt.leshendelse

import no.nav.pgi.skatt.leshendelse.maskinporten.createMaskinportenEnvVariables
import no.nav.pgi.skatt.leshendelse.mock.*
import no.nav.pgi.skatt.leshendelse.skatt.*
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue


@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PGILesHendelseSkattTest {
    private val grunnlagPgiHendelseClient = GrunnlagPgiHendelseClient(createEnvVariables())
    private val firstSekvensnummerClient = FirstSekvensnummerClient(createEnvVariables())

    private val skattClient = SkattClient(createEnvVariables())
    private val kafkaTestEnvironment = KafkaTestEnvironment()
    private val kafkaConfig = KafkaConfig(kafkaTestEnvironment.testConfiguration())
    private val sekvensnummerProducer = SekvensnummerProducer(kafkaConfig)
    private val sekvensnummerConsumer = SekvensnummerConsumer(kafkaConfig, TopicPartition(KafkaConfig.NEXT_SEKVENSNUMMER_TOPIC, 0))
    private val hendelseProducer = HendelseProducer(kafkaConfig)
    private val application = createApplication(kafkaConfig = kafkaConfig)

    private val sekvensnummerMock = SkattFirstSekvensnummerMock()
    private val hendelseMock = HendelseMock()
    private val maskinportenMock = MaskinportenMock()

    @BeforeAll
    internal fun init() {
        sekvensnummerMock.`mock first sekvensnummer endpoint`()
        hendelseMock.`stub hendelse endepunkt skatt`()
        maskinportenMock.`mock  maskinporten token enpoint`()
        application.start()
    }

    @AfterAll
    internal fun teardown() {
        application.stop(100, 100)
        kafkaTestEnvironment.tearDown()
        sekvensnummerMock.stop()
        hendelseMock.stop()
        maskinportenMock.stop()
        sekvensnummerConsumer.close()
    }

    @Test
    @Order(1)
    fun `get first sekvensnummer empty, call Skatteetaten REST service`() {
        var sekvensnummer = sekvensnummerConsumer.getLastSekvensnummer()
        if (sekvensnummer == null) {
            sekvensnummer = firstSekvensnummerClient.send().toString()
        }

        assertEquals("1", sekvensnummer)
    }

    @Test
    fun `get first sekvensnummer skatt`() {
        assertEquals(1L, firstSekvensnummerClient.send())
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
        val hendelser = grunnlagPgiHendelseClient.send(1000, 1L)
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

    private fun createEnvVariables() = createMaskinportenEnvVariables() +
            mapOf(
                    HENDELSE_HOST_ENV_KEY to HENDELSE_MOCK_HOST,
                    FIRST_SEKVENSNUMMER_HOST_ENV_KEY to FIRST_SEKVENSNUMMER_MOCK_HOST
            )
}
