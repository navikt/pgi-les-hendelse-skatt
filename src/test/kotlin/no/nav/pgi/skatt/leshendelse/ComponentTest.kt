package no.nav.pgi.skatt.leshendelse

import no.nav.pgi.domain.Hendelse
import no.nav.pgi.domain.serialization.PgiDomainSerializer
import no.nav.pgi.skatt.leshendelse.common.KafkaTestEnvironment
import no.nav.pgi.skatt.leshendelse.common.PlaintextStrategy
import no.nav.pgi.skatt.leshendelse.kafka.*
import no.nav.pgi.skatt.leshendelse.mock.*
import no.nav.pgi.skatt.leshendelse.mock.MaskinportenMock.Companion.MASKINPORTEN_ENV_VARIABLES
import no.nav.pgi.skatt.leshendelse.skatt.*
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class ComponentTest {
    private val kafkaTestEnvironment = KafkaTestEnvironment()
    private val kafkaFactory =
        KafkaHendelseFactory(KafkaConfig(kafkaTestEnvironment.kafkaTestEnvironmentVariables(), PlaintextStrategy()))
    private val sekvensnummerConsumer = SekvensnummerConsumer(
        consumer = kafkaFactory.nextSekvensnummerConsumer(),
        topicPartition = TopicPartition(NEXT_SEKVENSNUMMER_TOPIC, 0)
    )
    private val sekvensnummerProducer = SekvensnummerProducer(
        sekvensnummerProducer = kafkaFactory.nextSekvensnummerProducer()
    )

    private val hendelseMock = HendelseMock()
    private val maskinportenMock = MaskinportenMock()

    private val application = Application(kafkaFactory = kafkaFactory, env = createEnvVariables(), loopForever = false)

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
        val hendelser = hendelseMock.`stub hendelse endpoint second call`(
            currentSekvensnummer + antallHendelserFirstCall,
            antallHendelserSecondCall
        )
        application.startHendelseSkattLoop()

        assertThat(sekvensnummerConsumer.getNextSekvensnummer()!!.toLong()).isEqualTo(hendelser.getNextSekvensnummer())
        val hendelse =
            PgiDomainSerializer().fromJson(Hendelse::class, kafkaTestEnvironment.getLastRecordOnTopic().value())
        assertThat(hendelse).isEqualTo(hendelser[hendelser.size - 1].mapToHendelse())
    }

    private fun createEnvVariables() = MASKINPORTEN_ENV_VARIABLES +
            mapOf(
                HENDELSE_HOST_ENV_KEY to HENDELSE_MOCK_HOST,
                HENDELSE_PATH_ENV_KEY to HENDELSE_MOCK_PATH,
                FIRST_SEKVENSNUMMER_HOST_ENV_KEY to FIRST_SEKVENSNUMMER_MOCK_HOST,
                FIRST_SEKVENSNUMMER_PATH_ENV_KEY to FIRST_SEKVENSNUMMER_MOCK_PATH,
                SkattTimer.DELAY_IN_SECONDS_ENV_KEY to "0"
            )
}