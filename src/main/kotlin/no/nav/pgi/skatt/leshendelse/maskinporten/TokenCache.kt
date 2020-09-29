package no.nav.pgi.skatt.leshendelse.maskinporten

import com.nimbusds.jwt.SignedJWT
import java.util.*

internal class TokenCache {
    private var token: String? = null

    internal fun getToken(): String = token!!

    internal fun setToken(token: String) {
        this.token = token
    }

    internal fun isValidToken(): Boolean = (token != null) && Date() isMoreThan20SecondsBefore getExpirationTime()

    private fun getExpirationTime() = SignedJWT.parse(token).jwtClaimsSet.expirationTime as Date

    private infix fun Date.isMoreThan20SecondsBefore(date: Date): Boolean = (this.time - date.time) < -20000
}

