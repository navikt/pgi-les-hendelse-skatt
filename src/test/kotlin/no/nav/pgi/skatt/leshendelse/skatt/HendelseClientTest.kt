package no.nav.pgi.skatt.leshendelse.skatt


import no.nav.pgi.skatt.leshendelse.Sekvensnummer
import no.nav.pgi.skatt.leshendelse.mock.HENDELSE_MOCK_HOST
import no.nav.pgi.skatt.leshendelse.mock.HendelseMock
import no.nav.pgi.skatt.leshendelse.mock.MaskinportenMock
import no.nav.pgi.skatt.leshendelse.mock.MaskinportenMock.Companion.MASKINPORTEN_ENV_VARIABLES
import no.nav.pgi.skatt.leshendelse.mock.HENDELSE_MOCK_PATH
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals


private const val ANTALL_HENDELSER = 1000
private const val FRA_SEKVENSNUMMER = 1L

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GrunnlagPgiHendelseDtoClientTest {
    private val hendelseMock = HendelseMock()
    private val maskinportenMock = MaskinportenMock()
    private val client = HendelseClient(MASKINPORTEN_ENV_VARIABLES + createEnvVariables())

    @BeforeAll
    internal fun init() {
        maskinportenMock.`mock maskinporten token enpoint`()
    }

    @BeforeEach
    internal fun beforeEach() {
        hendelseMock.reset()
    }

    @AfterAll
    internal fun teardown() {
        hendelseMock.stop()
        maskinportenMock.stop()
    }

    @Test
    fun `returns hendelser`() {
        hendelseMock.`stub hendelse endpoint response with masked data from skatt`(FRA_SEKVENSNUMMER)

        val hendelser = client.getHendelserSkatt(ANTALL_HENDELSER, FRA_SEKVENSNUMMER)
        assertEquals(100, hendelser.size)
    }

    @Test
    fun `neste skevensummer should return USE_PREVIOUS_SEKVENSNUMMER when hendelser is empty`() {
        hendelseMock.`stub hendelse endpoint skatt`(FRA_SEKVENSNUMMER, 0)

        assertEquals(
            Sekvensnummer.USE_PREVIOUS,
            client.getHendelserSkatt(ANTALL_HENDELSER, FRA_SEKVENSNUMMER).getNextSekvensnummer()
        )
    }

    @Test
    fun `throw exception when response is not mappable`() {
        hendelseMock.`stub hendelse endpoint response that wont map`(FRA_SEKVENSNUMMER)
        assertThrows<HendelseClientObjectMapperException> {
            client.getHendelserSkatt(ANTALL_HENDELSER, FRA_SEKVENSNUMMER)
        }
    }

    @Test
    fun `accept unknown fields when mapping response from skatt`() {
        hendelseMock.`stub hendelse endpoint response with unknown fields from skatt`(FRA_SEKVENSNUMMER)
        val hendelser = client.getHendelserSkatt(ANTALL_HENDELSER, FRA_SEKVENSNUMMER)
        assertEquals(2, hendelser.size)
    }

    private fun createEnvVariables() = mapOf(
        HENDELSE_HOST_ENV_KEY to HENDELSE_MOCK_HOST,
        HENDELSE_PATH_ENV_KEY to HENDELSE_MOCK_PATH,
    )

}