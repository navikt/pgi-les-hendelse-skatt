package no.nav.pgi.skatt.leshendelse

import io.ktor.server.netty.*
import no.nav.pgi.skatt.leshendelse.maskinporten.createMaskinportenEnvVariables
import no.nav.pgi.skatt.leshendelse.mock.*
import no.nav.pgi.skatt.leshendelse.skatt.FIRST_SEKVENSNUMMER_HOST_ENV_KEY
import no.nav.pgi.skatt.leshendelse.skatt.HENDELSE_HOST_ENV_KEY
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class HendelseSkattComponentTest {
    private val kafkaTestEnvironment = KafkaTestEnvironment()
    private val kafkaConfig = KafkaConfig(kafkaTestEnvironment.kafkaTestEnvironmentVariables())
    private val sekvensnummerProducer = SekvensnummerProducer(kafkaConfig)
    private val sekvensnummerConsumer = SekvensnummerConsumer(kafkaConfig, TopicPartition(KafkaConfig.NEXT_SEKVENSNUMMER_TOPIC, 0))

    private val sekvensnummerMock = SkattFirstSekvensnummerMock()
    private val hendelseMock = HendelseMock()
    private val maskinportenMock = MaskinportenMock()

    private lateinit var application: NettyApplicationEngine
    private var currentSekvensnummer = 1L

    @BeforeAll
    internal fun init() {
        sekvensnummerMock.`mock first sekvensnummer endpoint`()
        maskinportenMock.`mock  maskinporten token enpoint`()
    }

    @AfterAll
    internal fun teardown() {
        application.stop(100, 100)
        kafkaTestEnvironment.tearDown()
        sekvensnummerMock.stop()
        hendelseMock.stop()
        maskinportenMock.stop()
    }

    @BeforeEach
    internal fun beforeEachTest() {
        application = createApplication(kafkaConfig = kafkaConfig, env = createEnvVariables())
        hendelseMock.reset()
    }

    @AfterEach
    internal fun afterEachTest() {
        currentSekvensnummer = sekvensnummerConsumer.getNextSekvensnummer()!!.toLong()
        application.stop(100, 100)
    }

    @Test
    fun `Should add sekvensnummer to topic`() {
        val antallHendelser = 79

        hendelseMock.`stub hendelse endepunkt skatt`(currentSekvensnummer, antallHendelser)
        application.start()
        assertEquals(currentSekvensnummer + antallHendelser, sekvensnummerConsumer.getNextSekvensnummer()!!.toLong())
    }

    @Test
    fun `Should continue to read hendelser when amount of hendelser is over threshold`() {
        val antallHendelserFirstCall = 1000
        val antallHendelserSecondCall = 100

        hendelseMock.`stub first call to hendelse endepunkt skatt`(currentSekvensnummer, antallHendelserFirstCall)
        val hendelser = hendelseMock.`stub second call to hendelse endepunkt skatt`(currentSekvensnummer + antallHendelserFirstCall, antallHendelserSecondCall)
        application.start()

        assertEquals(hendelser.hendelser[hendelser.hendelser.size - 1].mapToHendelse(), kafkaTestEnvironment.getLastRecordOnTopic().value())
    }

    private fun createEnvVariables() = createMaskinportenEnvVariables() +
            mapOf(
                    HENDELSE_HOST_ENV_KEY to HENDELSE_MOCK_HOST,
                    FIRST_SEKVENSNUMMER_HOST_ENV_KEY to FIRST_SEKVENSNUMMER_MOCK_HOST
            )

}