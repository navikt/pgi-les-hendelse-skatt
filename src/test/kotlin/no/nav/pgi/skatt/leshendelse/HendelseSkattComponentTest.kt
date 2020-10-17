package no.nav.pgi.skatt.leshendelse

import io.ktor.server.netty.*
import no.nav.pgi.skatt.leshendelse.kafka.KafkaConfig
import no.nav.pgi.skatt.leshendelse.kafka.NEXT_SEKVENSNUMMER_TOPIC
import no.nav.pgi.skatt.leshendelse.kafka.SekvensnummerConsumer
import no.nav.pgi.skatt.leshendelse.kafka.mapToHendelse
import no.nav.pgi.skatt.leshendelse.maskinporten.createMaskinportenEnvVariables
import no.nav.pgi.skatt.leshendelse.mock.*
import no.nav.pgi.skatt.leshendelse.skatt.FIRST_SEKVENSNUMMER_HOST_ENV_KEY
import no.nav.pgi.skatt.leshendelse.skatt.HENDELSE_HOST_ENV_KEY
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class HendelseSkattComponentTest {
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

    @BeforeEach
    internal fun beforeEachTest() {
        hendelseMock.reset()
        sekvensnummerMock.reset()
    }

    @AfterEach
    internal fun afterEachTest() {
        application.stopServer()
    }

    @AfterAll
    internal fun teardown() {
        application.stopServer()
        kafkaTestEnvironment.tearDown()
        sekvensnummerMock.stop()
        hendelseMock.stop()
        maskinportenMock.stop()
    }

    @Test
    @Order(1)
    fun `Should get sekvensnummer from FirstSekvensnummerClient when sekvensnummer topic is empty`() {
        val antallHendelser = 40
        val startingSekvensnummer = 40L

        assertEquals(null, sekvensnummerConsumer.getNextSekvensnummer())
        sekvensnummerMock.`mock first sekvensnummer endpoint`(startingSekvensnummer)

        val hendelserDto = hendelseMock.`stub hendelse endepunkt skatt`(startingSekvensnummer, antallHendelser)
        application.startHendelseSkattLoop(kafkaConfig = kafkaConfig, env = createEnvVariables(), loopForever = false)
        assertEquals(hendelserDto.nestesekvensnr, sekvensnummerConsumer.getNextSekvensnummer()!!.toLong())
    }

    @Test
    fun `Should continue to read hendelser when amount of hendelser is over threshold`() {
        val currentSekvensnummer = sekvensnummerConsumer.getNextSekvensnummer()?.toLong() ?: 1
        val antallHendelserFirstCall = 1000
        val antallHendelserSecondCall = 100

        hendelseMock.`stub first call to hendelse endepunkt skatt`(currentSekvensnummer, antallHendelserFirstCall)
        val hendelserDto = hendelseMock.`stub second call to hendelse endepunkt skatt`(currentSekvensnummer + antallHendelserFirstCall, antallHendelserSecondCall)
        application.startHendelseSkattLoop(kafkaConfig = kafkaConfig, env = createEnvVariables(), loopForever = false)

        assertEquals(hendelserDto.nestesekvensnr, sekvensnummerConsumer.getNextSekvensnummer()!!.toLong())
        assertEquals(hendelserDto.hendelser[hendelserDto.hendelser.size - 1].mapToHendelse(), kafkaTestEnvironment.getLastRecordOnTopic().value())
    }

    private fun createEnvVariables() = createMaskinportenEnvVariables() +
            mapOf(
                    HENDELSE_HOST_ENV_KEY to HENDELSE_MOCK_HOST,
                    FIRST_SEKVENSNUMMER_HOST_ENV_KEY to FIRST_SEKVENSNUMMER_MOCK_HOST
            )
}