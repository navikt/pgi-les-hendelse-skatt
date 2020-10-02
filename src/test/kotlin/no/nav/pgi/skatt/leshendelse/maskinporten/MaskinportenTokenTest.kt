package no.nav.pgi.skatt.leshendelse.maskinporten

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MaskinportenTokenTest {
    private val privateKey: RSAKey = RSAKeyGenerator(2048).keyID("123").generate()

    @Test
    internal fun `assert expired if expiration time on token is under 20 seconds from now`() {
        val token = MaskinportenToken(createMaskinportenToken(19))

        assertTrue(token.isExpired())
    }

    @Test
    internal fun `should not be expired if expiration time is over 20 seconds from now`() {
        val token = MaskinportenToken(createMaskinportenToken(30))

        assertFalse(token.isExpired())
    }

    private fun createMaskinportenToken(expiresIn: Int): String {
        val claimsSet = JWTClaimsSet.Builder()
                .expirationTime(Date(Date().getTime() + expiresIn * 1000))
                .build()
        val signedJWT = SignedJWT(
                JWSHeader.Builder(JWSAlgorithm.RS256).keyID(privateKey.getKeyID()).build(),
                claimsSet)
        signedJWT.sign(RSASSASigner(privateKey))
        return signedJWT.serialize()
    }
}