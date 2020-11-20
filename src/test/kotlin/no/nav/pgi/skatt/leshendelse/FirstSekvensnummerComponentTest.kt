package no.nav.pgi.skatt.leshendelse

import no.nav.pgi.skatt.leshendelse.common.KafkaTestEnvironment
import no.nav.pgi.skatt.leshendelse.common.PlaintextStrategy
import no.nav.pgi.skatt.leshendelse.kafka.KafkaConfig
import no.nav.pgi.skatt.leshendelse.kafka.NEXT_SEKVENSNUMMER_TOPIC
import no.nav.pgi.skatt.leshendelse.kafka.SekvensnummerConsumer
import no.nav.pgi.skatt.leshendelse.mock.*
import no.nav.pgi.skatt.leshendelse.skatt.FIRST_SEKVENSNUMMER_HOST_ENV_KEY
import no.nav.pgi.skatt.leshendelse.skatt.HENDELSE_HOST_ENV_KEY
import no.nav.pgi.skatt.leshendelse.skatt.getNextSekvensnummer
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FirstSekvensnummerComponentTest {
    private val kafkaTestEnvironment = KafkaTestEnvironment()
    private val kafkaConfig = KafkaConfig(kafkaTestEnvironment.kafkaTestEnvironmentVariables(), PlaintextStrategy())
    private val sekvensnummerConsumer = SekvensnummerConsumer(kafkaConfig, TopicPartition(NEXT_SEKVENSNUMMER_TOPIC, 0))

    private val sekvensnummerMock = SkattFirstSekvensnummerMock()
    private val hendelseMock = HendelseMock()
    private val maskinportenMock = MaskinportenMock()

    private val application = Application()

    @BeforeAll
    internal fun init() {
        maskinportenMock.`mock  maskinporten token enpoint`()
    }

    @AfterAll
    internal fun teardown() {
        application.stopServer()
        kafkaTestEnvironment.tearDown()
        hendelseMock.stop()
        maskinportenMock.stop()
        sekvensnummerMock.stop()
    }

    @Test
    @Order(1)
    fun `Should get sekvensnummer from FirstSekvensnummerClient when sekvensnummer topic is empty`() {
        val antallHendelser = 400
        val startingSekvensnummer = 1L

        sekvensnummerMock.`stub first sekvensnummer endpoint`(startingSekvensnummer)

        val hendelserDto = hendelseMock.`stub hendelse endpoint skatt`(startingSekvensnummer, antallHendelser)
        application.startHendelseSkattLoop(kafkaConfig = kafkaConfig, env = createEnvVariables(), loopForever = false)
        Assertions.assertEquals(hendelserDto.getNextSekvensnummer(), sekvensnummerConsumer.getNextSekvensnummer()!!.toLong())
    }

    private fun createEnvVariables() = MaskinportenMock.MASKINPORTEN_ENV_VARIABLES +
            mapOf(
                    HENDELSE_HOST_ENV_KEY to HENDELSE_MOCK_HOST,
                    FIRST_SEKVENSNUMMER_HOST_ENV_KEY to FIRST_SEKVENSNUMMER_MOCK_HOST,
                    MINUTES_TO_WAIT_BEFORE_CALLING_SKATT_ENV_KEY to "0.01"
            )
}