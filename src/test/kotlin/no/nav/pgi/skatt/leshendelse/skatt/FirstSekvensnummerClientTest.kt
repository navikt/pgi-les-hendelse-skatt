package no.nav.pgi.skatt.leshendelse.skatt

import com.github.tomakehurst.wiremock.client.WireMock
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import no.nav.pgi.skatt.leshendelse.maskinporten.*
import no.nav.pgi.skatt.leshendelse.mock.*
import no.nav.pgi.skatt.leshendelse.mock.FirstSekvensnummerMock
import no.nav.pgi.skatt.leshendelse.mock.HENDELSE_MOCK_HOST
import no.nav.pgi.skatt.leshendelse.mock.MASKINPORTEN_MOCK_HOST
import no.nav.pgi.skatt.leshendelse.mock.MaskinportenMock
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FirstSekvensnummerClientTest {
    private val firstSekvensnummerMock = FirstSekvensnummerMock()
    private val maskinportenMock = MaskinportenMock()
    private val firstSekvensnummerClient = FirstSekvensnummerClient(createEnvVariables())

    @BeforeAll
    internal fun init() {
        maskinportenMock.mockMaskinporten()
    }

    @AfterAll
    internal fun teardown() {
        firstSekvensnummerMock.stop()
        maskinportenMock.stop()
    }

    private fun createEnvVariables() = createMaskinportenEnvVariables() + mapOf(FIRST_SEKVENSNUMMER_HOST_ENV_KEY to FIRST_SEKVENSNUMMER_MOCK_HOST)
}