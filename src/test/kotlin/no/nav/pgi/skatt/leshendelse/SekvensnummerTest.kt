package no.nav.pgi.skatt.leshendelse

import no.nav.pgi.skatt.leshendelse.common.KafkaTestEnvironment
import no.nav.pgi.skatt.leshendelse.common.PlaintextStrategy
import no.nav.pgi.skatt.leshendelse.kafka.KafkaConfig
import no.nav.pgi.skatt.leshendelse.kafka.SekvensnummerProducer
import no.nav.pgi.skatt.leshendelse.mock.FIRST_SEKVENSNUMMER_MOCK_HOST
import no.nav.pgi.skatt.leshendelse.mock.MaskinportenMock
import no.nav.pgi.skatt.leshendelse.mock.SkattFirstSekvensnummerMock
import no.nav.pgi.skatt.leshendelse.skatt.FIRST_SEKVENSNUMMER_HOST_ENV_KEY
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
    internal fun `Sekvensnummer from skatt when sekvensnummer-topic is not populated`() {
        val firstSekvensnummer = 1L
        firstSekvensnummerMock.`stub first sekvensnummer endpoint`(firstSekvensnummer)

        assertEquals(firstSekvensnummer, sekvensnummer.value)
    }

    @Test
    internal fun `Sekvensnummer from topic when sekvensnummer-topic is populated`() {
        val sekvensnummerOnTopic = 20L
        addSekvensnummerToTopic(listOf(sekvensnummerOnTopic))

        assertEquals(sekvensnummerOnTopic, sekvensnummer.value)
    }

    @Test
    internal fun `Retrieves last sekvensnummer from topic when there are more then one instance on topic`() {
        val lastSekvensnummer = 51L
        val sekvensnummerList = (1..50).map { it.toLong() } + lastSekvensnummer
        addSekvensnummerToTopic(sekvensnummerList)

        assertEquals(lastSekvensnummer, sekvensnummer.value)
    }

    @Test
    internal fun `sets sekvensnummer`() {
        val newSekvensnummer = 100L
        sekvensnummer.value = newSekvensnummer

        assertEquals(newSekvensnummer, sekvensnummer.value)
    }

    //Todo Test på  legger på topic
    //TODO TEST leser ikke fra topic når den har begynt
    //LEgg inn topic konsumer for test.



    private fun addSekvensnummerToTopic(sekvensnummerList: List<Long>) = sekvensnummerList.forEach { sekvensnummerProducer.writeSekvensnummer(it) }

    private fun createEnvVariables() = MaskinportenMock.MASKINPORTEN_ENV_VARIABLES + mapOf(FIRST_SEKVENSNUMMER_HOST_ENV_KEY to FIRST_SEKVENSNUMMER_MOCK_HOST)
}