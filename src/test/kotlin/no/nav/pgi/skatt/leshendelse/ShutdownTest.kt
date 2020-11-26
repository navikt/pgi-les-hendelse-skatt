package no.nav.pgi.skatt.leshendelse

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import no.nav.pensjon.samhandling.liveness.IS_ALIVE_PATH
import no.nav.pgi.skatt.leshendelse.common.ExceptionKafkaProducer
import no.nav.pgi.skatt.leshendelse.common.KafkaMockFactory
import no.nav.pgi.skatt.leshendelse.mock.FIRST_SEKVENSNUMMER_MOCK_HOST
import no.nav.pgi.skatt.leshendelse.mock.HENDELSE_MOCK_HOST
import no.nav.pgi.skatt.leshendelse.mock.HendelseMock
import no.nav.pgi.skatt.leshendelse.mock.MaskinportenMock
import no.nav.pgi.skatt.leshendelse.skatt.FIRST_SEKVENSNUMMER_HOST_ENV_KEY
import no.nav.pgi.skatt.leshendelse.skatt.HENDELSE_HOST_ENV_KEY
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.lang.RuntimeException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers.ofString
import java.util.concurrent.ExecutionException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ShutdownTest {
    private val hendelseMock = HendelseMock()
    private val maskinportenMock = MaskinportenMock()
    private lateinit var kafkaMockFactory: KafkaMockFactory
    private lateinit var application: Application

    @BeforeAll
    internal fun init() {
        maskinportenMock.`mock maskinporten token enpoint`()
        hendelseMock.`stub hendelse endpoint skatt`(1)
    }

    @AfterEach
    internal fun AfterEach() {
        kafkaMockFactory.close()
        application.stopServer()
    }

    @AfterAll
    internal fun teardown() {
        kafkaMockFactory.close()
        hendelseMock.stop()
        maskinportenMock.stop()
        application.stopServer()
    }

    private fun createEnvVariables() = MaskinportenMock.MASKINPORTEN_ENV_VARIABLES +
            mapOf(
                    HENDELSE_HOST_ENV_KEY to HENDELSE_MOCK_HOST,
                    FIRST_SEKVENSNUMMER_HOST_ENV_KEY to FIRST_SEKVENSNUMMER_MOCK_HOST,
                    MINUTES_TO_WAIT_BEFORE_CALLING_SKATT_ENV_KEY to "0.01"
            )

    @Test
    fun `should close produsers and consumers when close is called from outside application`() {
        kafkaMockFactory = KafkaMockFactory()
        application = Application(kafkaFactory = kafkaMockFactory, env = createEnvVariables(), loopForever = true)

        GlobalScope.async {
            delay(100)
            application.stopServer()
        }

        assertThrows<Exception> { application.startHendelseSkattLoop() }

        Assertions.assertTrue(kafkaMockFactory.nextSekvensnummerConsumer.closed())
        Assertions.assertTrue(kafkaMockFactory.nextSekvensnummerProducer.closed())
        Assertions.assertTrue(kafkaMockFactory.hendelseProducer.closed())
    }

    @Test
    fun `should close naisServer when close is called from outside application`() {
        kafkaMockFactory = KafkaMockFactory()
        application = Application(kafkaFactory = kafkaMockFactory, env = createEnvVariables(), loopForever = true)

        GlobalScope.async {
            application.startHendelseSkattLoop()
        }

        Thread.sleep(100)
        assertEquals(200, callIsAlive().statusCode())

        application.stopServer()
        Thread.sleep(100)
        assertThrows<Exception> { callIsAlive() }
    }

    @Test
    fun `should throw unhandled exception out of application`() {
        kafkaMockFactory = KafkaMockFactory(hendelseProducer = ExceptionKafkaProducer())
        application = Application(kafkaFactory = kafkaMockFactory, env = createEnvVariables(), loopForever = true)

        assertThrows<Throwable>{application.startHendelseSkattLoop()}
    }


    private fun callIsAlive() =
            HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8080$IS_ALIVE_PATH")).GET().build(), ofString())

}

