package no.nav.pgi.skatt.leshendelse.maskinporten

import com.nimbusds.jwt.SignedJWT
import java.util.*

private const val TWENTY_SECONDS = 20

internal class MaskinportenToken(private val token: String? = null) {

    internal fun getTokenString(): String = token!!

    internal fun isExpired(): Boolean {
        if (token == null) return true
        return !token.getTokenExpirationTime().is20SecondsEarlierThenNow()
    }
}


private fun Date.is20SecondsEarlierThenNow(): Boolean = this.timeInSeconds() - (now().timeInSeconds() + TWENTY_SECONDS) >= 0
private fun Date.timeInSeconds() = this.time / 1000
private fun now() = Date()
private fun String.getTokenExpirationTime() = SignedJWT.parse(this).jwtClaimsSet.expirationTime as Date

