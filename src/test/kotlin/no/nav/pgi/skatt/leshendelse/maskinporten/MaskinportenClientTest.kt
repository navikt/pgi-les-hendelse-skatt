package no.nav.pgi.skatt.leshendelse.maskinporten

import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import no.nav.pgi.skatt.leshendelse.MissingEnvironmentVariables
import no.nav.pgi.skatt.leshendelse.maskinporten.mock.MASKINPORTEN_MOCK_HOST
import no.nav.pgi.skatt.leshendelse.maskinporten.mock.MaskinportenMock
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MaskinportenClientTest {
    private lateinit var maskinportenClient: MaskinportenClient
    private val privateKey: RSAKey = RSAKeyGenerator(2048).keyID("123").generate()
    private var maskinportenMock: MaskinportenMock = MaskinportenMock()

    @BeforeAll
    internal fun init() {
        maskinportenClient = MaskinportenClient(createEnvVariables())
    }

    @BeforeEach
    internal fun beforeEach() {
        maskinportenMock.reset()
    }

    @AfterAll
    internal fun teardown() {
        maskinportenMock.close()
    }

    @Test
    fun `reuse token if not expired`() {
        maskinportenMock.mockOnlyOneCall()

        val firstToken = maskinportenClient.getToken()
        val secondToken = maskinportenClient.getToken()
        assertEquals(firstToken, secondToken)
    }

    @Test
    fun `throws MaskinportenObjectMapperException if response from maskinporten cant be mapped`() {
        maskinportenMock.mockNonJSON()

        assertThrows<MaskinportenObjectMapperException> { maskinportenClient.getToken() }
    }

    @Test
    fun `Throwing error when environment variables are missing`() {
        val exception = assertThrows<MissingEnvironmentVariables> { MaskinportenClient(emptyMap()) }

        Assertions.assertTrue(exception.message!! containWord MASKINPORTEN_TOKEN_HOST_ENV_KEY)
        Assertions.assertTrue(exception.message containWord AUDIENCE_ENV_KEY)
        Assertions.assertTrue(exception.message containWord ISSUER_ENV_KEY)
        Assertions.assertTrue(exception.message containWord SCOPE_ENV_KEY)
        Assertions.assertTrue(exception.message containWord VALID_IN_SECONDS_ENV_KEY)
        Assertions.assertTrue(exception.message containWord PRIVATE_JWK_ENV_KEY)
    }

    private fun createEnvVariables() = mapOf(
            AUDIENCE_ENV_KEY to "testAud",
            ISSUER_ENV_KEY to "testIssuer",
            SCOPE_ENV_KEY to "testScope",
            VALID_IN_SECONDS_ENV_KEY to "120",
            PRIVATE_JWK_ENV_KEY to privateKey.toJSONString(),
            MASKINPORTEN_TOKEN_HOST_ENV_KEY to MASKINPORTEN_MOCK_HOST
    )

    private infix fun String.containWord(word: String) = this.contains(word)
}