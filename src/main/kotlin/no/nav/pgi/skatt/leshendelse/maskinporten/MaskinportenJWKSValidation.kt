package no.nav.pgi.skatt.leshendelse.maskinporten

import com.nimbusds.jose.util.DefaultResourceRetriever
import no.nav.pgi.skatt.leshendelse.getVal
import no.nav.security.token.support.core.validation.ConfigurableJwtTokenValidator
import java.net.URI
import java.net.URL

private const val ISSUER = "maskinporten"

private const val MASKINPORTEN_JWKS_URL = "maskinporten-jwks-well-known-url"
//"https://ver2.maskinporten.no/.well-known/oauth-authorization-server"


internal class MaskinportenJWKSValidation(env: Map<String, String>) {
    private val resourceRetriever = DefaultResourceRetriever()
    private var jwksUrl: URL = URI.create(env.getVal(MASKINPORTEN_JWKS_URL)).toURL()
    private var tokenValidator: ConfigurableJwtTokenValidator

    init {
        this.tokenValidator = ConfigurableJwtTokenValidator(ISSUER, jwksUrl, resourceRetriever, emptyList())
    }

    internal fun validateToken(token: String) {
        tokenValidator.assertValidToken(token)
    }
}