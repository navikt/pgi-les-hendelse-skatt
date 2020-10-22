package no.nav.pgi.skatt.leshendelse.maskinporten

import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import no.nav.pensjon.samhandling.env.MissingEnvironmentVariables
import no.nav.pgi.skatt.leshendelse.mock.MASKINPORTEN_MOCK_HOST
import no.nav.pgi.skatt.leshendelse.mock.MaskinportenMock
import no.nav.security.maskinporten.client.exceptions.MaskinportenClientException
import no.nav.security.maskinporten.client.exceptions.MaskinportenObjectMapperException
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MaskinportenTest {
    private var maskinportenMock: MaskinportenMock = MaskinportenMock()

    @BeforeEach
    internal fun beforeEach() {
        maskinportenMock.reset()
    }

    @AfterAll
    internal fun teardown() {
        maskinportenMock.stop()
    }

    @Test
    fun `reuse token from maskinporten if not expired`() {
        val maskinporten = Maskinporten(createMaskinportenEnvVariables())
        maskinportenMock.`mock valid response for only one call`()

        val firstToken = maskinporten.token
        val secondToken = maskinporten.token
        assertEquals(firstToken, secondToken)
    }

    @Test
    fun `throws MaskinportenObjectMapperException if response from maskinporten cant be mapped`() {
        val maskinporten = Maskinporten(createMaskinportenEnvVariables())
        maskinportenMock.`mock invalid JSON response`()

        assertThrows<MaskinportenObjectMapperException> { maskinporten.token }
    }

    @Test
    fun `Throws MaskinportenClientException when status other than 200 is returned from maskinporten`() {
        val maskinporten = Maskinporten(createMaskinportenEnvVariables())
        maskinportenMock.`mock 500 server error`()

        assertThrows<MaskinportenClientException> { maskinporten.token }
    }

    @Test
    fun `Throws MissingEnvironmentVariables when environment variables are missing`() {
        val exception = assertThrows<MissingEnvironmentVariables> { Maskinporten(emptyMap()) }

        Assertions.assertTrue(exception.message!! containWord MASKINPORTEN_TOKEN_HOST_ENV_KEY)
        Assertions.assertTrue(exception.message!! containWord SCOPE_ENV_KEY)
        Assertions.assertTrue(exception.message!! containWord VALID_IN_SECONDS_ENV_KEY)
        Assertions.assertTrue(exception.message!! containWord PRIVATE_JWK_ENV_KEY)
    }

    private infix fun String.containWord(word: String) = this.contains(word)
}

internal fun createMaskinportenEnvVariables(privateKey: RSAKey = RSAKeyGenerator(2048).keyID("123").generate()) = mapOf(
        SCOPE_ENV_KEY to "testScope",
        CLIENT_ID_ENV_KEY to "testClient",
        VALID_IN_SECONDS_ENV_KEY to "120",
        PRIVATE_JWK_ENV_KEY to privateKey.toJSONString(),
        MASKINPORTEN_TOKEN_HOST_ENV_KEY to MASKINPORTEN_MOCK_HOST
)