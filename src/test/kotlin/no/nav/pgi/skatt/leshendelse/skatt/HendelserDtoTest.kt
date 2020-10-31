package no.nav.pgi.skatt.leshendelse.skatt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class HendelserDtoTest {

    @Test
    fun `HendelserDto returns USE_PREVIOUS_SEKVENSNUMMER when there is no hendelseDto`() {
        assertEquals(USE_PREVIOUS_SEKVENSNUMMER, HendelserDto().getNesteSekvensnummer())
    }

    @Test
    fun `HendelserDto returns size of hendelse-list`() {
        val hendelseList: List<HendelseDto> = listOf(HendelseDto("12345678901", "2020", 3))
        assertEquals(hendelseList.size, HendelserDto(hendelseList).size())
    }

}