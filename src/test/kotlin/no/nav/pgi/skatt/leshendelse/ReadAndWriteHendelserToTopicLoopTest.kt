package no.nav.pgi.skatt.leshendelse

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.pgi.domain.Hendelse
import no.nav.pgi.domain.serialization.PgiDomainSerializer
import no.nav.pgi.skatt.leshendelse.kafka.HendelseProducerException
import no.nav.pgi.skatt.leshendelse.mock.ExceptionKafkaProducer
import no.nav.pgi.skatt.leshendelse.mock.KafkaMockFactory
import no.nav.pgi.skatt.leshendelse.kafka.NEXT_SEKVENSNUMMER_TOPIC
import no.nav.pgi.skatt.leshendelse.kafka.SekvensnummerConsumer
import no.nav.pgi.skatt.leshendelse.mock.FIRST_SEKVENSNUMMER_MOCK_HOST
import no.nav.pgi.skatt.leshendelse.mock.FIRST_SEKVENSNUMMER_MOCK_PATH
import no.nav.pgi.skatt.leshendelse.mock.HENDELSE_MOCK_HOST
import no.nav.pgi.skatt.leshendelse.mock.HendelseMock
import no.nav.pgi.skatt.leshendelse.mock.MaskinportenMock
import no.nav.pgi.skatt.leshendelse.mock.HENDELSE_MOCK_PATH
import no.nav.pgi.skatt.leshendelse.skatt.FIRST_SEKVENSNUMMER_HOST_ENV_KEY
import no.nav.pgi.skatt.leshendelse.skatt.FIRST_SEKVENSNUMMER_PATH_ENV_KEY
import no.nav.pgi.skatt.leshendelse.skatt.HENDELSE_HOST_ENV_KEY
import no.nav.pgi.skatt.leshendelse.skatt.HENDELSE_PATH_ENV_KEY
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.InterruptException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ReadAndWriteHendelserToTopicLoopTest {

    private val hendelseMock = HendelseMock()
    private val maskinportenMock = MaskinportenMock()
    private lateinit var kafkaMockFactory: KafkaMockFactory
    private lateinit var readAndWriteLoop: ReadAndWriteHendelserToTopicLoop

    @BeforeAll
    internal fun init() {
        maskinportenMock.`mock maskinporten token enpoint`()
    }

    @AfterAll
    internal fun teardown() {
        hendelseMock.stop()
        maskinportenMock.stop()
        readAndWriteLoop.close()
        kafkaMockFactory.close()
    }

    @AfterEach
    internal fun afterEach() {
        hendelseMock.reset()
        kafkaMockFactory.close()
        readAndWriteLoop.close()
    }

    @Test
    fun `adds less then 1000 hendelser to topic and exits loop`() {
        val hendelseCount = 400
        val fraSekvensnummer = 1L

        kafkaMockFactory = KafkaMockFactory()
        readAndWriteLoop = ReadAndWriteHendelserToTopicLoop(
            counters = Counters(SimpleMeterRegistry()),
            kafkaFactory = kafkaMockFactory,
            env = createEnvVariables()
        )
        hendelseMock.`stub hendelse endpoint skatt`(fraSekvensnummer, hendelseCount)

        assertDoesNotThrow { readAndWriteLoop.processHendelserFromSkattWhileAboveThreshold() }

        val hendelseProducerHistory = kafkaMockFactory.hendelseProducer.history()
        val nextSekvensnummerHistory = kafkaMockFactory.nextSekvensnummerProducer.history()

        assertThat(hendelseProducerHistory).hasSize(hendelseCount)
        assertEquals(fraSekvensnummer.toString(), nextSekvensnummerHistory[0].value())
        val hendelse = PgiDomainSerializer().fromJson(Hendelse::class, hendelseProducerHistory.last().value())
        assertEquals(
            (hendelse.sekvensnummer + 1).toString(),
            nextSekvensnummerHistory[1].value()
        )
    }

    @Test
    fun `should throw exception if read sekvensnummer throws exception`() {
        val failingConsumer = MockConsumer<String, String>(OffsetResetStrategy.LATEST)
            .apply {
                assign(listOf(TopicPartition(NEXT_SEKVENSNUMMER_TOPIC, 0)))
                updateEndOffsets(mapOf(SekvensnummerConsumer.defaultTopicPartition to 2))
                setPollException(InterruptException("Test exception"))
            }

        kafkaMockFactory = KafkaMockFactory(nextSekvensnummerConsumer = failingConsumer)
        readAndWriteLoop = ReadAndWriteHendelserToTopicLoop(
            Counters(meterRegistry = SimpleMeterRegistry()),
            kafkaMockFactory,
            createEnvVariables()
        )

        assertThrows<InterruptException> { readAndWriteLoop.processHendelserFromSkattWhileAboveThreshold() }
    }

    @Test
    fun `should use sekvensnummer of failing hendelse when exception is thrown while hendelser is added to topic`() {
        val failingProducer = ExceptionKafkaProducer()

        kafkaMockFactory = KafkaMockFactory(hendelseProducer = failingProducer)
        readAndWriteLoop = ReadAndWriteHendelserToTopicLoop(
            counters = Counters(meterRegistry = SimpleMeterRegistry()),
            kafkaFactory = kafkaMockFactory,
            env = createEnvVariables()
        )

        val hendelser = hendelseMock.`stub hendelse endpoint skatt`(1, 15)

        assertThrows<HendelseProducerException> { readAndWriteLoop.processHendelserFromSkattWhileAboveThreshold() }

        assertEquals(
            hendelser[0].sekvensnummer.toString(),
            kafkaMockFactory.nextSekvensnummerProducer.history().last().value()
        )
    }

    private fun createEnvVariables() =
        mapOf(
            HENDELSE_HOST_ENV_KEY to HENDELSE_MOCK_HOST,
            HENDELSE_PATH_ENV_KEY to HENDELSE_MOCK_PATH,
            FIRST_SEKVENSNUMMER_HOST_ENV_KEY to FIRST_SEKVENSNUMMER_MOCK_HOST,
            FIRST_SEKVENSNUMMER_PATH_ENV_KEY to FIRST_SEKVENSNUMMER_MOCK_PATH,
        ) + MaskinportenMock.MASKINPORTEN_ENV_VARIABLES
}
