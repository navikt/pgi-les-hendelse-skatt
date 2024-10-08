package no.nav.pgi.skatt.leshendelse

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.pgi.skatt.leshendelse.common.KafkaTestEnvironment
import no.nav.pgi.skatt.leshendelse.common.PlaintextStrategy
import no.nav.pgi.skatt.leshendelse.kafka.*
import no.nav.pgi.skatt.leshendelse.mock.FIRST_SEKVENSNUMMER_MOCK_HOST
import no.nav.pgi.skatt.leshendelse.mock.FIRST_SEKVENSNUMMER_MOCK_PATH
import no.nav.pgi.skatt.leshendelse.mock.MaskinportenMock
import no.nav.pgi.skatt.leshendelse.mock.SkattFirstSekvensnummerMock
import no.nav.pgi.skatt.leshendelse.skatt.FIRST_SEKVENSNUMMER_HOST_ENV_KEY
import no.nav.pgi.skatt.leshendelse.skatt.FIRST_SEKVENSNUMMER_PATH_ENV_KEY
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SekvensnummerTest {
    private val kafkaTestEnvironment = KafkaTestEnvironment()
    private val kafkaFactory =
        KafkaFactoryImpl(KafkaConfig(kafkaTestEnvironment.kafkaTestEnvironmentVariables(), PlaintextStrategy()))
    private val maskinportenMock = MaskinportenMock()
    private val firstSekvensnummerMock = SkattFirstSekvensnummerMock()
    private val sekvensnummerProducer = SekvensnummerProducer(
        counters = Counters(SimpleMeterRegistry()),
        sekvensnummerProducer = kafkaFactory.nextSekvensnummerProducer(),
    )
    private val sekvensnummerConsumer = SekvensnummerConsumer(
        consumer = kafkaFactory.nextSekvensnummerConsumer(),
        topicPartition = TopicPartition(NEXT_SEKVENSNUMMER_TOPIC, 0)
    )

    private var sekvensnummer = Sekvensnummer(
        counters = Counters(SimpleMeterRegistry()),
        kafkaFactory = kafkaFactory,
        env = createEnvVariables()
    )

    @BeforeAll
    internal fun init() {
        maskinportenMock.`mock maskinporten token enpoint`()
    }

    @AfterEach
    internal fun beforeEach() {
        firstSekvensnummerMock.reset()
        sekvensnummer.close()
        sekvensnummer = Sekvensnummer(
            counters = Counters(SimpleMeterRegistry()),
            kafkaFactory = kafkaFactory,
            env = createEnvVariables()
        )
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
        assertThrows<Exception> { sekvensnummer.getSekvensnummer() }
    }

    @Test
    @Order(1)
    internal fun `Gets sekvensnummer from skatt when sekvensnummer-topic is not populated`() {
        val firstSekvensnummer = 1L
        firstSekvensnummerMock.`stub first sekvensnummer endpoint`(firstSekvensnummer)

        assertEquals(firstSekvensnummer, sekvensnummer.getSekvensnummer())
    }

    @Test
    internal fun `Gets sekvensnummer from topic when sekvensnummer-topic is populated`() {
        val sekvensnummerOnTopic = 20L
        addSekvensnummerToTopic(listOf(sekvensnummerOnTopic))

        assertEquals(sekvensnummerOnTopic, sekvensnummer.getSekvensnummer())
    }

    @Test
    internal fun `Gets last sekvensnummer from topic when there are more then one instance on topic`() {
        val lastSekvensnummer = 21L
        val sekvensnummerList = (1..20).map { it.toLong() } + lastSekvensnummer
        addSekvensnummerToTopic(sekvensnummerList)

        assertEquals(lastSekvensnummer, sekvensnummer.getSekvensnummer())
    }

    @Test
    internal fun `adds sekvensnummer to topic when sekvensnummer value is set`() {
        val newSekvensnummer = 100L
        sekvensnummer.setSekvensnummer(newSekvensnummer)

        Thread.sleep(600)
        assertEquals(newSekvensnummer, sekvensnummerConsumer.getNextSekvensnummer()?.toLong())
    }

    @Test
    internal fun `Using local chash instead of topic after retrieving sekvensnummer ones from topic`() {
        val firstSekvensnummer = 75L
        addSekvensnummerToTopic(listOf(firstSekvensnummer))
        assertEquals(firstSekvensnummer, sekvensnummer.getSekvensnummer())

        val cashedSekvensnummer = 76L
        val sekvensnummerAddedDirectlyToTopic = 77L
        sekvensnummer.setSekvensnummer(cashedSekvensnummer)
        addSekvensnummerToTopic(listOf(sekvensnummerAddedDirectlyToTopic))

        assertEquals(cashedSekvensnummer, sekvensnummer.getSekvensnummer())
    }

    @Test
    internal fun `should use previous sekvensnummer when sekvensnummer is less than zero`() {
        val validSekvensnummer = 0L
        val invalidSekvensnummer = Sekvensnummer.USE_PREVIOUS
        sekvensnummer.setSekvensnummer(validSekvensnummer)
        sekvensnummer.setSekvensnummer(invalidSekvensnummer)

        Thread.sleep(600)
        assertEquals(validSekvensnummer, sekvensnummer.getSekvensnummer())
        assertEquals(validSekvensnummer, sekvensnummerConsumer.getNextSekvensnummer()?.toLong())
    }

    @Test
    internal fun `resets sekvensnummer to earliest possible`() {
        val firstSekvensnummer = 14L
        firstSekvensnummerMock.`stub first sekvensnummer endpoint`(firstSekvensnummer)

        val sekvensnummerOnTopic = 20L
        val env = createEnvVariables() + mapOf(
            "TILBAKESTILL_SEKVENSNUMMER" to "true",
        )
        addSekvensnummerToTopic(listOf(sekvensnummerOnTopic))
        assertEquals(sekvensnummerOnTopic, sekvensnummer.getSekvensnummer())

        sekvensnummer = Sekvensnummer(
            counters = Counters(SimpleMeterRegistry()),
            kafkaFactory = kafkaFactory,
            env = env
        )

        assertEquals(14, sekvensnummer.getSekvensnummer())
    }

    @Test
    internal fun `resets sekvensnummer to specific date`() {
        val firstSekvensnummer = 15L
        firstSekvensnummerMock.`stub first sekvensnummer endpoint med dato`(firstSekvensnummer)

        val sekvensnummerOnTopic = 20L
        val env = createEnvVariables() + mapOf(
            "TILBAKESTILL_SEKVENSNUMMER" to "true",
            "TILBAKESTILL_SEKVENSNUMMER_TIL" to "2023-06-01",
        )
        addSekvensnummerToTopic(listOf(sekvensnummerOnTopic))
        assertEquals(sekvensnummerOnTopic, sekvensnummer.getSekvensnummer())

        sekvensnummer = Sekvensnummer(
            counters = Counters(SimpleMeterRegistry()),
            kafkaFactory = kafkaFactory,
            env = env
        )

        assertEquals(15, sekvensnummer.getSekvensnummer())
    }

    @Test
    internal fun `does not reset if disabled`() {
        val firstSekvensnummer = 15L
        firstSekvensnummerMock.`stub first sekvensnummer endpoint med dato`(firstSekvensnummer)

        val sekvensnummerOnTopic = 20L
        val env = createEnvVariables() + mapOf(
            "TILBAKESTILL_SEKVENSNUMMER" to "false",
        )
        addSekvensnummerToTopic(listOf(sekvensnummerOnTopic))
        assertEquals(sekvensnummerOnTopic, sekvensnummer.getSekvensnummer())

        sekvensnummer = Sekvensnummer(Counters(SimpleMeterRegistry()), kafkaFactory, env)

        assertEquals(20, sekvensnummer.getSekvensnummer())
    }

    private fun addSekvensnummerToTopic(sekvensnummerList: List<Long>) =
        sekvensnummerList.forEach { sekvensnummerProducer.writeSekvensnummer(it) }

    private fun createEnvVariables() = MaskinportenMock.MASKINPORTEN_ENV_VARIABLES + mapOf(
        FIRST_SEKVENSNUMMER_HOST_ENV_KEY to FIRST_SEKVENSNUMMER_MOCK_HOST,
        FIRST_SEKVENSNUMMER_PATH_ENV_KEY to FIRST_SEKVENSNUMMER_MOCK_PATH,
    )
}