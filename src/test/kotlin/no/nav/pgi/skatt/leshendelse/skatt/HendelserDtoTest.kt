package no.nav.pgi.skatt.leshendelse.skatt

import no.nav.pgi.skatt.leshendelse.Sekvensnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class HendelserDtoTest {

    @Test
    fun `HendelserDto returns USE_PREVIOUS_SEKVENSNUMMER when there is no hendelseDto`() {
        assertEquals(Sekvensnummer.USE_PREVIOUS, HendelserDto().getNextSekvensnummer())
    }

    @Test
    fun `HendelserDto returns size of hendelse-list`() {
        val hendelseList: List<HendelseDto> = listOf(HendelseDto("12345678901", "2020", 3))
        assertEquals(hendelseList.size, HendelserDto(hendelseList).size())
    }

    @Test
    fun `HendelserDto returns first sekvensnummer when fistSekvensnummer is called`() {
        val firstSekvensnummer = 3L

        val hendelseList: List<HendelseDto> = listOf(
                HendelseDto("12345678901", "2020", firstSekvensnummer),
                HendelseDto("12345678901", "2020", 4L)
        )

        assertEquals(firstSekvensnummer, HendelserDto(hendelseList).fistSekvensnummer())
    }

    @Test
    fun `HendelserDto returns last sekvensnummer when lastSekvensnummer is called`() {
        val lastSekvensnummer = 4L

        val hendelseList: List<HendelseDto> = listOf(
                HendelseDto("12345678901", "2020", 3L),
                HendelseDto("12345678901", "2020", lastSekvensnummer)
        )

        assertEquals(lastSekvensnummer, HendelserDto(hendelseList).lastSekvensnummer())
    }

    @Test
    fun `HendelserDto fistSekvensnummer returns null when hendelseList is empty`() {
        assertEquals(null, HendelserDto(emptyList()).fistSekvensnummer())
    }

    @Test
    fun `HendelserDto lastSekvensnummer returns null when hendelseList is empty`() {
        assertEquals(null, HendelserDto(emptyList()).lastSekvensnummer())
    }

    @Test
    fun `HendelserDto amountOfHendelserBefore returns hendelser before sekvensnummer`() {
        val sekvensnummer = 3L

        val hendelseList: List<HendelseDto> = listOf(
                HendelseDto("12345678901", "2020", 1L),
                HendelseDto("12345678901", "2020", 2L),
                HendelseDto("12345678901", "2020", sekvensnummer)
        )

        assertEquals(2, HendelserDto(hendelseList).amountOfHendelserBefore(sekvensnummer))
    }

    @Test
    fun `HendelserDto amountOfHendelserBefore returns 0 if element was not found`() {
        val sekvensnummer = 3L

        val hendelseList: List<HendelseDto> = listOf(
                HendelseDto("12345678901", "2020", 1L),
                HendelseDto("12345678901", "2020", 2L)
        )

        assertEquals(0, HendelserDto(hendelseList).amountOfHendelserBefore(sekvensnummer))
    }

    @Test
    fun `HendelserDto amountOfHendelserBefore returns 0 if list is empty`() {
        assertEquals(0, HendelserDto(emptyList()).amountOfHendelserBefore(3L))
    }

    @Test
    fun `HendelserDto returns sekvensnummer + 1 when get nextSekvensnummer`() {
        val sekvensnummer = 3L
        val hendelseList: List<HendelseDto> = listOf(HendelseDto("12345678901", "2020", sekvensnummer))
        assertEquals(sekvensnummer + 1, HendelserDto(hendelseList).getNextSekvensnummer())
    }
}