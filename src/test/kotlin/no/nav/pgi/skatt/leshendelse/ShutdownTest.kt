package no.nav.pgi.skatt.leshendelse

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import no.nav.pgi.skatt.leshendelse.mock.*
import no.nav.pgi.skatt.leshendelse.skatt.FIRST_SEKVENSNUMMER_HOST_ENV_KEY
import no.nav.pgi.skatt.leshendelse.skatt.FIRST_SEKVENSNUMMER_PATH_ENV_KEY
import no.nav.pgi.skatt.leshendelse.skatt.HENDELSE_HOST_ENV_KEY
import no.nav.pgi.skatt.leshendelse.skatt.HENDELSE_PATH_ENV_KEY
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ShutdownTest {
    private val hendelseMock = HendelseMock()
    private val maskinportenMock = MaskinportenMock()
    private lateinit var kafkaMockFactory: KafkaMockFactory
    private lateinit var applicationService: ApplicationService

    @BeforeAll
    internal fun init() {
        maskinportenMock.`mock maskinporten token enpoint`()
    }

    @AfterEach
    internal fun afterEach() {
        kafkaMockFactory.close()
        applicationService.stopServer()
        hendelseMock.reset()
    }

    @AfterAll
    internal fun teardown() {
        kafkaMockFactory.close()
        hendelseMock.stop()
        maskinportenMock.stop()
        applicationService.stopServer()
    }

    @Test
    fun `should close produsers and consumers when close is called from outside application`() {
        kafkaMockFactory = KafkaMockFactory()
        applicationService = ApplicationService(
            counters = Counters(SimpleMeterRegistry()),
            kafkaFactory = kafkaMockFactory,
            env = createEnvVariables(),
            loopForever = true
        )
        hendelseMock.`stub hendelse endpoint skatt`()

        GlobalScope.async {
            delay(100)
            applicationService.stopServer()
        }

        assertThatThrownBy {
            applicationService.startHendelseSkattLoop()
        }
            .isInstanceOf(Exception::class.java)

        assertThat(kafkaMockFactory.nextSekvensnummerConsumer.closed()).isTrue()
        assertThat(kafkaMockFactory.nextSekvensnummerProducer.closed()).isTrue()
        assertThat(kafkaMockFactory.hendelseProducer.closed()).isTrue()
    }

    /*
    @Test
    fun `should close naisServer when close is called from outside application`() {
        kafkaMockFactory = KafkaMockFactory()
        application = Application(
            kafkaFactory = kafkaMockFactory,
            env = createEnvVariables(),
            loopForever = true
        )
        hendelseMock.`stub hendelse endpoint skatt`()

        GlobalScope.async {
            application.startHendelseSkattLoop()
        }

        Thread.sleep(100)
        assertThat(callIsAlive().statusCode()).isEqualTo(200)

        application.stopServer()
        Thread.sleep(100)
        assertThatThrownBy {
            callIsAlive()
        }
            .isInstanceOf(Exception::class.java)
    }
     */

    @Test
    fun `should throw unhandled exception out of application`() {
        hendelseMock.`stub hendelse endpoint skatt`()
        kafkaMockFactory = KafkaMockFactory(
            hendelseProducer = ExceptionKafkaProducer()
        )
        applicationService = ApplicationService(
            counters = Counters(SimpleMeterRegistry()),
            kafkaFactory = kafkaMockFactory,
            env = createEnvVariables(),
            loopForever = true
        )

        assertThatThrownBy {
            applicationService.startHendelseSkattLoop()
        }
            .isInstanceOf(Throwable::class.java)
    }


    @Test
    fun `should close when waiting to call skatt`() {
        kafkaMockFactory = KafkaMockFactory()
        hendelseMock.`stub hendelse endpoint skatt`(1, 10)
        hendelseMock.`stub hendelse endpoint skatt`(11, 10)
        val envVariables =
            mapOf(
                HENDELSE_HOST_ENV_KEY to HENDELSE_MOCK_HOST,
                HENDELSE_PATH_ENV_KEY to HENDELSE_MOCK_PATH,
                FIRST_SEKVENSNUMMER_HOST_ENV_KEY to FIRST_SEKVENSNUMMER_MOCK_HOST,
                SkattTimer.DELAY_IN_SECONDS_ENV_KEY to "180"
            ) + MaskinportenMock.MASKINPORTEN_ENV_VARIABLES

        applicationService = ApplicationService(
            counters = Counters(SimpleMeterRegistry()),
            kafkaFactory = kafkaMockFactory,
            env = envVariables,
            loopForever = true
        )

        GlobalScope.async {
            delay(100)
            applicationService.stopServer()
        }

        assertThatThrownBy {
            applicationService.startHendelseSkattLoop()
        }
            .isInstanceOf(Throwable::class.java)
    }

    private fun createEnvVariables() = MaskinportenMock.MASKINPORTEN_ENV_VARIABLES +
            mapOf(
                HENDELSE_HOST_ENV_KEY to HENDELSE_MOCK_HOST,
                HENDELSE_PATH_ENV_KEY to HENDELSE_MOCK_PATH,
                FIRST_SEKVENSNUMMER_HOST_ENV_KEY to FIRST_SEKVENSNUMMER_MOCK_HOST,
                FIRST_SEKVENSNUMMER_PATH_ENV_KEY to FIRST_SEKVENSNUMMER_MOCK_PATH,
                SkattTimer.DELAY_IN_SECONDS_ENV_KEY to "0"
            )
    /*
    private fun callIsAlive() =
        HttpClient.newHttpClient().send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080$IS_ALIVE_PATH")).GET().build(), ofString()
        )
     */

}

