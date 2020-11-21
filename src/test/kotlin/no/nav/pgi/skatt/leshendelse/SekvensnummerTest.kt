package no.nav.pgi.skatt.leshendelse

import no.nav.pgi.skatt.leshendelse.common.KafkaTestEnvironment
import no.nav.pgi.skatt.leshendelse.common.PlaintextStrategy
import no.nav.pgi.skatt.leshendelse.kafka.KafkaConfig
import no.nav.pgi.skatt.leshendelse.kafka.NEXT_SEKVENSNUMMER_TOPIC
import no.nav.pgi.skatt.leshendelse.kafka.SekvensnummerConsumer
import no.nav.pgi.skatt.leshendelse.kafka.SekvensnummerProducer
import no.nav.pgi.skatt.leshendelse.mock.FIRST_SEKVENSNUMMER_MOCK_HOST
import no.nav.pgi.skatt.leshendelse.mock.MaskinportenMock
import no.nav.pgi.skatt.leshendelse.mock.SkattFirstSekvensnummerMock
import no.nav.pgi.skatt.leshendelse.skatt.FIRST_SEKVENSNUMMER_HOST_ENV_KEY
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SekvensnummerTest {
    private val kafkaTestEnvironment = KafkaTestEnvironment()
    private val kafkaConfig = KafkaConfig(kafkaTestEnvironment.kafkaTestEnvironmentVariables(), PlaintextStrategy())
    private val maskinportenMock = MaskinportenMock()
    private val firstSekvensnummerMock = SkattFirstSekvensnummerMock()
    private val sekvensnummerProducer = SekvensnummerProducer(kafkaConfig)
    private val sekvensnummerConsumer = SekvensnummerConsumer(kafkaConfig, TopicPartition(NEXT_SEKVENSNUMMER_TOPIC, 0))

    private var sekvensnummer = Sekvensnummer(kafkaConfig, createEnvVariables())

    @BeforeAll
    internal fun init() {
        maskinportenMock.`mock maskinporten token enpoint`()
    }

    @AfterEach
    internal fun beforeEach() {
        firstSekvensnummerMock.reset()
        sekvensnummer.close()
        sekvensnummer = Sekvensnummer(kafkaConfig, createEnvVariables())
    }

    @AfterAll
    internal fun teardown() {
        sekvensnummerProducer.close()
        sekvensnummer.close()
        sekvensnummerConsumer.close()

        maskinportenMock.stop()
        firstSekvensnummerMock.stop()

        kafkaTestEnvironment.tearDown()
    }

    @Test
    @Order(0)
    internal fun `fails with exception when first sekvensnummer from skatt does not return 200`() {
        firstSekvensnummerMock.`mock 404 response`()
        assertThrows<Exception> { sekvensnummer.value }
    }

    @Test
    @Order(1)
    internal fun `Gets sekvensnummer from skatt when sekvensnummer-topic is not populated`() {
        val firstSekvensnummer = 1L
        firstSekvensnummerMock.`stub first sekvensnummer endpoint`(firstSekvensnummer)

        assertEquals(firstSekvensnummer, sekvensnummer.value)
    }

    @Test
    internal fun `Gets sekvensnummer from topic when sekvensnummer-topic is populated`() {
        val sekvensnummerOnTopic = 20L
        addSekvensnummerToTopic(listOf(sekvensnummerOnTopic))

        assertEquals(sekvensnummerOnTopic, sekvensnummer.value)
    }

    @Test
    internal fun `Gets last sekvensnummer from topic when there are more then one instance on topic`() {
        val lastSekvensnummer = 51L
        val sekvensnummerList = (1..50).map { it.toLong() } + lastSekvensnummer
        addSekvensnummerToTopic(sekvensnummerList)

        assertEquals(lastSekvensnummer, sekvensnummer.value)
    }

    @Test
    internal fun `adds sekvensnummer to topic when sekvensnummer value is set`() {
        val newSekvensnummer = 100L
        sekvensnummer.value = newSekvensnummer

        assertEquals(newSekvensnummer, sekvensnummerConsumer.getNextSekvensnummer()?.toLong())
    }

    @Test
    internal fun `Using local chash instead of topic after retrieving sekvensnummer ones from topic`() {
        val firstSekvensnummer = 75L
        addSekvensnummerToTopic(listOf(firstSekvensnummer))
        assertEquals(firstSekvensnummer, sekvensnummer.value)

        val cashedSekvensnummer = 76L
        val sekvensnummerAddedDirectlyToTopic = 77L
        sekvensnummer.value = cashedSekvensnummer
        addSekvensnummerToTopic(listOf(sekvensnummerAddedDirectlyToTopic))

        assertEquals(cashedSekvensnummer, sekvensnummer.value)
    }

    @Test
    internal fun `should use previous sekvensnummer when sekvensnummer is less than zero`() {
        val validSekvensnummer = 0L
        val invalidSekvensnummer = Sekvensnummer.USE_PREVIOUS
        sekvensnummer.value = validSekvensnummer
        sekvensnummer.value = invalidSekvensnummer

        assertEquals(validSekvensnummer, sekvensnummer.value)
        assertEquals(validSekvensnummer, sekvensnummerConsumer.getNextSekvensnummer()?.toLong())
    }

    private fun addSekvensnummerToTopic(sekvensnummerList: List<Long>) = sekvensnummerList.forEach { sekvensnummerProducer.writeSekvensnummer(it) }

    private fun createEnvVariables() = MaskinportenMock.MASKINPORTEN_ENV_VARIABLES + mapOf(FIRST_SEKVENSNUMMER_HOST_ENV_KEY to FIRST_SEKVENSNUMMER_MOCK_HOST)
}