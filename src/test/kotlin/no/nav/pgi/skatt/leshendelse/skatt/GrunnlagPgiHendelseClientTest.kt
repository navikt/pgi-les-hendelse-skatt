package no.nav.pgi.skatt.leshendelse.skatt

import no.nav.pgi.skatt.leshendelse.maskinporten.createMaskinportenEnvVariables
import no.nav.pgi.skatt.leshendelse.mock.HENDELSE_MOCK_HOST
import no.nav.pgi.skatt.leshendelse.mock.HendelseMock
import no.nav.pgi.skatt.leshendelse.mock.MaskinportenMock
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GrunnlagPgiHendelseClientTest {
    private val hendelseMock = HendelseMock()
    private val maskinportenMock = MaskinportenMock()
    private val client = GrunnlagPgiHendelseClient(createMaskinportenEnvVariables() + createEnvVariables())

    @BeforeAll
    internal fun init() {
        maskinportenMock.mockMaskinporten()
    }

    @AfterAll
    internal fun teardown() {
        hendelseMock.stop()
        maskinportenMock.stop()
    }

    @Test
    fun `returns hendelser`() {
        val antall = 1
        val fraSekvensnummer = 1L
        hendelseMock.`stub for skatt hendelser`(antall, fraSekvensnummer)

        val hendelser = client.send(antall, fraSekvensnummer)

        assertEquals(hendelser.size(), 5)
    }

    @Test
    fun `throw exception when nestesekvensnr is missing from response`() {
        val antall = 2
        val fraSekvensnummer = 1L
        hendelseMock.`stub response without nestesekvensnr`(antall, fraSekvensnummer)

        assertThrows<GrunnlagPgiHendelserValidationException> {
            client.send(antall, fraSekvensnummer)
        }
    }

    @Test
    fun `throw exception when response is not mappable`() {
        val antall = 3
        val fraSekvensnummer = 1L
        hendelseMock.`stub response that wont map`(antall, fraSekvensnummer)

        assertThrows<GrunnlagPgiHendelseClientObjectMapperException> {
            client.send(antall, fraSekvensnummer)
        }
    }


    private fun createEnvVariables() = mapOf(
            HENDELSE_HOST_ENV_KEY to HENDELSE_MOCK_HOST
    )

}