package no.nav.pgi.skatt.leshendelse.skatt

import no.nav.pgi.skatt.leshendelse.Sekvensnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class HendelseDtoTest {

    @Test
    fun `HendelserDtoWrapper returns empty list if no hendelseDto is added`() {
        assertEquals(true, HendelserDtoWrapper().hendelser.isEmpty())
    }

    @Test
    fun `HendelseDto getNextSekvensnummer returns USE_PREVIOUS_SEKVENSNUMMER when empty`() {
        assertEquals(Sekvensnummer.USE_PREVIOUS, emptyList<HendelseDto>().getNextSekvensnummer())
    }

    @Test
    fun `HendelseDto list returns first sekvensnummer when fistSekvensnummer is called`() {
        val firstSekvensnummer = 3L

        val hendelseList: List<HendelseDto> = listOf(
            HendelseDto("12345678901", "2020", firstSekvensnummer),
            HendelseDto("12345678901", "2020", 4L)
        )

        assertEquals(firstSekvensnummer, hendelseList.fistSekvensnummer())
    }

    @Test
    fun `HendelseDto list returns last sekvensnummer when lastSekvensnummer is called`() {
        val lastSekvensnummer = 4L

        val hendelseList: List<HendelseDto> = listOf(
            HendelseDto("12345678901", "2020", 3L),
            HendelseDto("12345678901", "2020", lastSekvensnummer)
        )

        assertEquals(lastSekvensnummer, hendelseList.lastSekvensnummer())
    }

    @Test
    fun `HendelseDto list fistSekvensnummer returns null when empty`() {
        assertEquals(null, emptyList<HendelseDto>().fistSekvensnummer())
    }

    @Test
    fun `HendelseDto list lastSekvensnummer returns null when empty`() {
        assertEquals(null, emptyList<HendelseDto>().lastSekvensnummer())
    }

    @Test
    fun `HendelserDto amountOfHendelserBefore returns amount of hendelser before sekvensnummer`() {
        val sekvensnummer = 3L

        val hendelseList: List<HendelseDto> = listOf(
            HendelseDto("12345678901", "2020", 1L),
            HendelseDto("12345678901", "2020", 2L),
            HendelseDto("12345678901", "2020", sekvensnummer)
        )

        assertEquals(2, hendelseList.amountOfHendelserBefore(sekvensnummer))
    }

    @Test
    fun `HendelseDto list amountOfHendelserBefore returns 0 if element was not found`() {
        val sekvensnummer = 3L

        val hendelseList: List<HendelseDto> = listOf(
            HendelseDto("12345678901", "2020", 1L),
            HendelseDto("12345678901", "2020", 2L)
        )

        assertEquals(0, hendelseList.amountOfHendelserBefore(sekvensnummer))
    }

    @Test
    fun `HendelseDto list amountOfHendelserBefore returns 0 if list is empty`() {
        assertEquals(0, emptyList<HendelseDto>().amountOfHendelserBefore(3L))
    }

    @Test
    fun `HendelseDto list returns sekvensnummer + 1 when get nextSekvensnummer`() {
        val sekvensnummer = 3L
        val hendelseList: List<HendelseDto> = listOf(HendelseDto("12345678901", "2020", sekvensnummer))
        assertEquals(sekvensnummer + 1, hendelseList.getNextSekvensnummer())
    }
}