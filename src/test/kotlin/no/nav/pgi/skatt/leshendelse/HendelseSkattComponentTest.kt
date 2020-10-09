package no.nav.pgi.skatt.leshendelse

import no.nav.pgi.skatt.leshendelse.maskinporten.createMaskinportenEnvVariables
import no.nav.pgi.skatt.leshendelse.mock.*
import no.nav.pgi.skatt.leshendelse.skatt.FIRST_SEKVENSNUMMER_HOST_ENV_KEY
import no.nav.pgi.skatt.leshendelse.skatt.HENDELSE_HOST_ENV_KEY
import no.nav.pgi.skatt.leshendelse.skatt.HendelseDto
import no.nav.pgi.skatt.leshendelse.skatt.HendelserDto
import org.junit.jupiter.api.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class HendelseSkattComponentTest {
    private val kafkaTestEnvironment = KafkaTestEnvironment()
    private val kafkaConfig = KafkaConfig(kafkaTestEnvironment.kafkaTestEnvironmentVariables())

    private val sekvensnummerMock = SkattFirstSekvensnummerMock()
    private val hendelseMock = HendelseMock()
    private val maskinportenMock = MaskinportenMock()

    private val application = createApplication(kafkaConfig = kafkaConfig, env = createEnvVariables())

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

    @Disabled
    @Test
    fun should() {
        hendelseMock.`stub first call to hendelse endepunkt skatt`(1 ,1000)
        hendelseMock.`stub second call to hendelse endepunkt skatt`(1000 ,100)
        application.start()
        kafkaTestEnvironment.getFirstRecordOnTopic()
    }



    private fun createEnvVariables() = createMaskinportenEnvVariables() +
            mapOf(
                    HENDELSE_HOST_ENV_KEY to HENDELSE_MOCK_HOST,
                    FIRST_SEKVENSNUMMER_HOST_ENV_KEY to FIRST_SEKVENSNUMMER_MOCK_HOST
            )

}