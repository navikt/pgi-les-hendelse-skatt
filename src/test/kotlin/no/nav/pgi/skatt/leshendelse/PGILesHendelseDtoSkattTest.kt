package no.nav.pgi.skatt.leshendelse

import no.nav.pgi.skatt.leshendelse.kafka.*
import no.nav.pgi.skatt.leshendelse.maskinporten.createMaskinportenEnvVariables
import no.nav.pgi.skatt.leshendelse.mock.*
import no.nav.pgi.skatt.leshendelse.skatt.*
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals


@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PGILesHendelseDtoSkattTest {
    private val grunnlagPgiHendelseClient = HendelseClient(createEnvVariables())
    private val firstSekvensnummerClient = FirstSekvensnummerClient(createEnvVariables())

    private val kafkaTestEnvironment = KafkaTestEnvironment()
    private val kafkaConfig = KafkaConfig(kafkaTestEnvironment.kafkaTestEnvironmentVariables(), PlaintextStrategy())
    private val sekvensnummerProducer = SekvensnummerProducer(kafkaConfig)
    private val sekvensnummerConsumer = SekvensnummerConsumer(kafkaConfig, TopicPartition(NEXT_SEKVENSNUMMER_TOPIC, 0))
    private val hendelseProducer = HendelseProducer(kafkaConfig)
    private val application = Application()

    private val sekvensnummerMock = SkattFirstSekvensnummerMock()
    private val hendelseMock = HendelseMock()
    private val maskinportenMock = MaskinportenMock()

    @BeforeAll
    internal fun init() {
        sekvensnummerMock.`mock first sekvensnummer endpoint`()
        hendelseMock.`stub hendelse response with masked data from skatt`(1000, 1)
        maskinportenMock.`mock  maskinporten token enpoint`()
    }

    @AfterAll
    internal fun teardown() {
        application.stopServer()

        kafkaTestEnvironment.tearDown()
        sekvensnummerConsumer.close()

        sekvensnummerMock.stop()
        hendelseMock.stop()
        maskinportenMock.stop()
    }

    @Test
    @Order(1)
    fun `get first sekvensnummer empty, call Skatteetaten REST service`() {
        var sekvensnummer = sekvensnummerConsumer.getNextSekvensnummer()
        if (sekvensnummer == null) {
            sekvensnummer = firstSekvensnummerClient.getFirstSekvensnummerFromSkatt().toString()
        }

        assertEquals("1", sekvensnummer)
    }

    @Test
    fun `get first sekvensnummer skatt`() {
        assertEquals(1L, firstSekvensnummerClient.getFirstSekvensnummerFromSkatt())
    }


    @Test
    fun `get last sekvensnummer from topic`() {
        val lastSekvensnummer = "5"
        addListOfSekvensnummerToTopic(listOf("1", "2", "3", "4", lastSekvensnummer))

        assertEquals(lastSekvensnummer, sekvensnummerConsumer.getNextSekvensnummer())
    }

    @Test
    fun `write sekvensnummer to topic`() {
        sekvensnummerProducer.writeSekvensnummer(1234L)
        assertEquals("1234", sekvensnummerConsumer.getNextSekvensnummer())
    }


    @Test
    fun `get hendelser from skatt`() {
        val hendelser = grunnlagPgiHendelseClient.getHendelserSkatt(1000, 1L)
        assertEquals(100, hendelser.size())
    }

    @Test
    fun `write pgi hendelse to topic`() {
        val hendelse = HendelseDto("123456", "12345", 1L)
        hendelseProducer.writeHendelse(hendelse)
        val record = kafkaTestEnvironment.getFirstRecordOnTopic()

        assertEquals(hendelse.identifikator, record.key().getIdentifikator())
        assertEquals(hendelse.gjelderPeriode, record.key().getGjelderPeriode())

        assertEquals(hendelse.sekvensnummer, record.value().getSekvensnummer())
        assertEquals(hendelse.identifikator, record.value().getIdentifikator())
        assertEquals(hendelse.gjelderPeriode, record.value().getGjelderPeriode())
    }

    private fun addListOfSekvensnummerToTopic(sekvensnummerList: List<String>) {
        sekvensnummerList.indices.forEach { i -> sekvensnummerProducer.writeSekvensnummer(sekvensnummerList[i].toLong()) }
    }

    private fun createEnvVariables() = createMaskinportenEnvVariables() + mapOf(
            HENDELSE_HOST_ENV_KEY to HENDELSE_MOCK_HOST,
            FIRST_SEKVENSNUMMER_HOST_ENV_KEY to FIRST_SEKVENSNUMMER_MOCK_HOST
    )
}
