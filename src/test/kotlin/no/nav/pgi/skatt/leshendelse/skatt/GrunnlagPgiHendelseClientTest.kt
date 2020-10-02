package no.nav.pgi.skatt.leshendelse.skatt

import no.nav.pgi.skatt.leshendelse.maskinporten.createMaskinportenEnvVariables
import no.nav.pgi.skatt.leshendelse.mock.HENDELSE_MOCK_HOST
import no.nav.pgi.skatt.leshendelse.mock.HendelseMock
import no.nav.pgi.skatt.leshendelse.mock.MaskinportenMock
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals


private const val ANTALL_HENDELSER = 1
private const val FRA_SEKVENSNUMMER = 1L

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GrunnlagPgiHendelseClientTest {
    private val hendelseMock = HendelseMock()
    private val maskinportenMock = MaskinportenMock()
    private val client = GrunnlagPgiHendelseClient(createMaskinportenEnvVariables() + createEnvVariables())

    @BeforeAll
    internal fun init() {
        maskinportenMock.`mock  maskinporten token enpoint`()
    }

    @BeforeEach
    internal fun beforeEach(){
        hendelseMock.reset()
    }

    @AfterAll
    internal fun teardown() {
        hendelseMock.stop()
        maskinportenMock.stop()
    }

    @Test
    fun `returns hendelser`() {
        hendelseMock.`stub for skatt hendelser`(ANTALL_HENDELSER, FRA_SEKVENSNUMMER)

        val hendelser = client.send(ANTALL_HENDELSER, FRA_SEKVENSNUMMER)
        assertEquals(hendelser.size(), 5)
    }

    @Test
    fun `throw exception when nestesekvensnr is missing from response`() {
        hendelseMock.`stub response without nestesekvensnr`(ANTALL_HENDELSER, FRA_SEKVENSNUMMER)

        assertThrows<GrunnlagPgiHendelserValidationException> {
            client.send(ANTALL_HENDELSER, FRA_SEKVENSNUMMER)
        }
    }

    @Test
    fun `throw exception when response is not mappable`() {
        hendelseMock.`stub response that wont map`(ANTALL_HENDELSER, FRA_SEKVENSNUMMER)

        assertThrows<GrunnlagPgiHendelseClientObjectMapperException> {
            client.send(ANTALL_HENDELSER, FRA_SEKVENSNUMMER)
        }
    }


    private fun createEnvVariables() = mapOf(
            HENDELSE_HOST_ENV_KEY to HENDELSE_MOCK_HOST
    )

}