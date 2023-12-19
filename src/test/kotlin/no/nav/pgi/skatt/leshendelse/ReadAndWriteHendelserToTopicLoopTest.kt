package no.nav.pgi.skatt.leshendelse

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
import no.nav.samordning.pgi.schema.Hendelse
import no.nav.samordning.pgi.schema.HendelseKey
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.InterruptException
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.concurrent.ExecutionException


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
    internal fun AfterEach() {
        hendelseMock.reset()
        kafkaMockFactory.close()
        readAndWriteLoop.close()
    }

    @Test
    fun `adds less then 1000 hendelser to topic and exits loop`() {
        val hendelseCount = 400
        val fraSekvensnummer = 1L

        kafkaMockFactory = KafkaMockFactory()
        readAndWriteLoop = ReadAndWriteHendelserToTopicLoop(kafkaMockFactory, createEnvVariables())
        hendelseMock.`stub hendelse endpoint skatt`(fraSekvensnummer, hendelseCount)

        assertDoesNotThrow { readAndWriteLoop.start() }

        val hendelseProducerHistory = kafkaMockFactory.hendelseProducer.history()
        val nextSekvensnummerHistory = kafkaMockFactory.nextSekvensnummerProducer.history()

        assertEquals(hendelseCount, hendelseProducerHistory.size)
        assertEquals(fraSekvensnummer.toString(), nextSekvensnummerHistory[0].value())
        assertEquals(
            (hendelseProducerHistory.last().value().getSekvensnummer() + 1).toString(),
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
        readAndWriteLoop = ReadAndWriteHendelserToTopicLoop(kafkaMockFactory, createEnvVariables())

        assertThrows<InterruptException> { readAndWriteLoop.start() }
    }

    @Test
    fun `should use sekvensnummer of failing hendelse when exception is thrown while hendelser is added to topic`() {
        val failingProducer = ExceptionKafkaProducer<HendelseKey, Hendelse>()

        kafkaMockFactory = KafkaMockFactory(hendelseProducer = failingProducer)
        readAndWriteLoop = ReadAndWriteHendelserToTopicLoop(kafkaMockFactory, createEnvVariables())

        val hendelser = hendelseMock.`stub hendelse endpoint skatt`(1, 15)

        assertThrows<ExecutionException> { readAndWriteLoop.start() }

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
