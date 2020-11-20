package no.nav.pgi.skatt.leshendelse.skatt

import no.nav.pgi.skatt.leshendelse.mock.FIRST_SEKVENSNUMMER_MOCK_HOST
import no.nav.pgi.skatt.leshendelse.mock.MaskinportenMock
import no.nav.pgi.skatt.leshendelse.mock.MaskinportenMock.Companion.MASKINPORTEN_ENV_VARIABLES
import no.nav.pgi.skatt.leshendelse.mock.SkattFirstSekvensnummerMock
import org.junit.jupiter.api.*


@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FirstSekvensnummerClientTest {
    private val firstSekvensnummerMock = SkattFirstSekvensnummerMock()
    private val maskinportenMock = MaskinportenMock()
    private val firstSekvensnummerClient = FirstSekvensnummerClient(createEnvVariables())

    @BeforeAll
    internal fun init() {
        maskinportenMock.`mock maskinporten token enpoint`()
    }

    @BeforeEach
    internal fun beforeEach() {
        firstSekvensnummerMock.reset()
    }

    @AfterAll
    internal fun teardown() {
        firstSekvensnummerMock.stop()
        maskinportenMock.stop()
    }

    @Test
    fun `get first sekvensnummer skatt`() {
        firstSekvensnummerMock.`stub first sekvensnummer endpoint`()
        Assertions.assertEquals(1L, firstSekvensnummerClient.getFirstSekvensnummer())
    }

    @Test
    fun `Throws FirstSekvensnummerClientCallException when other status than 200`() {
        firstSekvensnummerMock.`mock 404 response`()
        assertThrows<FirstSekvensnummerClientCallException> { firstSekvensnummerClient.getFirstSekvensnummer() }
    }

    @Test
    fun `Throws FirstSekvensnummerClientMappingException mapping fails`() {
        firstSekvensnummerMock.`mock faulty json response`()
        assertThrows<FirstSekvensnummerClientMappingException> { firstSekvensnummerClient.getFirstSekvensnummer() }
    }

    @Test
    fun `Throws FirstSekvensnummerClientMappingException first sekvensnummer is missing`() {
        firstSekvensnummerMock.`mock response without first sekvensnummer`()
        assertThrows<FirstSekvensnummerClientMappingException> { firstSekvensnummerClient.getFirstSekvensnummer() }
    }

    private fun createEnvVariables() = MASKINPORTEN_ENV_VARIABLES + mapOf(FIRST_SEKVENSNUMMER_HOST_ENV_KEY to FIRST_SEKVENSNUMMER_MOCK_HOST)
}