package no.nav.pgi.skatt.leshendelse.skatt

import no.nav.pgi.skatt.leshendelse.maskinporten.createMaskinportenEnvVariables
import no.nav.pgi.skatt.leshendelse.mock.FIRST_SEKVENSNUMMER_MOCK_HOST
import no.nav.pgi.skatt.leshendelse.mock.MaskinportenMock
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
        maskinportenMock.`mock  maskinporten token enpoint`()
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
        firstSekvensnummerMock.`mock first sekvensnummer endpoint`()
        Assertions.assertEquals(1L, firstSekvensnummerClient.send())
    }

    @Test
    fun `Throws FirstSekvensnummerClientCallException when other status than 200`() {
        firstSekvensnummerMock.`mock 404 response`()
        assertThrows<FirstSekvensnummerClientCallException> { firstSekvensnummerClient.send() }
    }

    @Test
    fun `Throws FirstSekvensnummerClientMappingException mapping fails`() {
        firstSekvensnummerMock.`mock faulty json response`()
        assertThrows<FirstSekvensnummerClientMappingException> { firstSekvensnummerClient.send() }
    }

    @Test
    fun `Throws FirstSekvensnummerClientMappingException first sekvensnummer is missing`() {
        firstSekvensnummerMock.`mock response without first sekvensnummer`()
        assertThrows<FirstSekvensnummerClientMappingException> { firstSekvensnummerClient.send() }
    }

    private fun createEnvVariables() = createMaskinportenEnvVariables() + mapOf(FIRST_SEKVENSNUMMER_HOST_ENV_KEY to FIRST_SEKVENSNUMMER_MOCK_HOST)
}