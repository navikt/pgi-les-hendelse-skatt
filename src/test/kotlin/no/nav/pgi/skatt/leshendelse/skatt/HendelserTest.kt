package no.nav.pgi.skatt.leshendelse.skatt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HendelserTest {

    @Test
    fun test() {
        val inntektsAar = "2018"
        val identifikator = "12345678901"

        val expectedHendelseKey = "$identifikator-$inntektsAar"
        assertEquals(expectedHendelseKey, HendelseDto(identifikator, inntektsAar, 1L).getHendelseKey())
    }
}