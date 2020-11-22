package no.nav.pgi.skatt.leshendelse

import no.nav.pgi.skatt.leshendelse.common.KafkaTestEnvironment
import no.nav.pgi.skatt.leshendelse.common.PlaintextStrategy
import no.nav.pgi.skatt.leshendelse.kafka.*
import no.nav.pgi.skatt.leshendelse.mock.*
import no.nav.pgi.skatt.leshendelse.mock.MaskinportenMock.Companion.MASKINPORTEN_ENV_VARIABLES
import no.nav.pgi.skatt.leshendelse.skatt.FIRST_SEKVENSNUMMER_HOST_ENV_KEY
import no.nav.pgi.skatt.leshendelse.skatt.HENDELSE_HOST_ENV_KEY
import no.nav.pgi.skatt.leshendelse.skatt.getNextSekvensnummer
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class ReadAndWriteHendelserToTopicComponentTest {
    private val kafkaTestEnvironment = KafkaTestEnvironment()
    private val kafkaConfig = KafkaConfig(kafkaTestEnvironment.kafkaTestEnvironmentVariables(), PlaintextStrategy())
    private val sekvensnummerConsumer = SekvensnummerConsumer(kafkaConfig, TopicPartition(NEXT_SEKVENSNUMMER_TOPIC, 0))
    private val sekvensnummerProducer = SekvensnummerProducer(kafkaConfig)

    private val hendelseMock = HendelseMock()
    private val maskinportenMock = MaskinportenMock()

    private val application = Application(kafkaConfig = kafkaConfig, env = createEnvVariables(), loopForever = false)

    @BeforeAll
    internal fun init() {
        maskinportenMock.`mock maskinporten token enpoint`()
    }

    @AfterAll
    internal fun teardown() {
        application.stopServer()
        kafkaTestEnvironment.tearDown()
        hendelseMock.stop()
        maskinportenMock.stop()
    }

    @Test
    fun `Should continue to read hendelser when amount of hendelser is over threshold`() {
        val currentSekvensnummer = 10L
        sekvensnummerProducer.writeSekvensnummer(currentSekvensnummer)

        val antallHendelserFirstCall = 1000
        val antallHendelserSecondCall = 60

        hendelseMock.`stub hendelse endpoint first call`(currentSekvensnummer, antallHendelserFirstCall)
        val hendelserDto = hendelseMock.`stub hendelse endpoint second call`(currentSekvensnummer + antallHendelserFirstCall, antallHendelserSecondCall)
        application.startHendelseSkattLoop()

        assertEquals(hendelserDto.getNextSekvensnummer(), sekvensnummerConsumer.getNextSekvensnummer()!!.toLong())
        assertEquals(hendelserDto.hendelser[hendelserDto.hendelser.size - 1].mapToHendelse(), kafkaTestEnvironment.getLastRecordOnTopic().value())
    }

    private fun createEnvVariables() = MASKINPORTEN_ENV_VARIABLES +
            mapOf(
                    HENDELSE_HOST_ENV_KEY to HENDELSE_MOCK_HOST,
                    FIRST_SEKVENSNUMMER_HOST_ENV_KEY to FIRST_SEKVENSNUMMER_MOCK_HOST,
                    MINUTES_TO_WAIT_BEFORE_CALLING_SKATT_ENV_KEY to "0.01"
            )
}