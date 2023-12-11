package no.nav.pgi.skatt.leshendelse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.Month

class TilbakestillSekvensnummerTest {
    @Test
    fun `reset fra første mulige hvis ingen dato spesifisert`() {
        val reset = TilbakestillSekvensnummer(env = mapOf("TILBAKESTILL_SEKVENSNUMMER" to "true"))
        assertEquals(true, reset.skalTilbakestille())
        assertEquals(HentSekvensnummer.FørsteMulige, reset.hentFra())
    }

    @Test
    fun `reset til dato hvis dato spesifisert`() {
        val reset = TilbakestillSekvensnummer(
            env = mapOf(
                "TILBAKESTILL_SEKVENSNUMMER" to "true",
                "TILBAKESTILL_SEKVENSNUMMER_TIL" to "2023-06-01"
            )
        )
        assertEquals(true, reset.skalTilbakestille())
        assertEquals(HentSekvensnummer.FraDato(LocalDate.of(2023, Month.JUNE, 1)), reset.hentFra())
    }

    @Test
    fun `reset er disabled by default`() {
        val reset = TilbakestillSekvensnummer(env = mapOf())
        assertEquals(false, reset.skalTilbakestille())
        assertThrows<IllegalArgumentException> { reset.hentFra() }
    }
}