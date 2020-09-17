package no.nav.pgi.skatt.leshendelse.skatt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HendelserTest {

    @Test
    fun test() {
        val inntektsAar = "1234"
        val identifikator = "12345678901"
        val expectedHendelseKey = "$identifikator-$inntektsAar"
        assertEquals(expectedHendelseKey, Hendelse(1, identifikator, inntektsAar).getHendelseKey())
    }
}