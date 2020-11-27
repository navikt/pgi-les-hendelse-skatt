package no.nav.pgi.skatt.leshendelse.skatt

import no.nav.pgi.skatt.leshendelse.Sekvensnummer
import kotlin.math.max

data class HendelserDto(val hendelser: List<HendelseDto> = ArrayList())
data class HendelseDto(val identifikator: String, val gjelderPeriode: String, val sekvensnummer: Long)

internal fun HendelseDto.getHendelseKey(): String = "$identifikator-$gjelderPeriode"
internal fun HendelserDto.getNextSekvensnummer() = if (hendelser.isEmpty()) Sekvensnummer.USE_PREVIOUS else hendelser[hendelser.size - 1].sekvensnummer + 1
internal fun HendelserDto.size() = hendelser.size
internal fun HendelserDto.fistSekvensnummer() = if (hendelser.isEmpty()) null else hendelser.first().sekvensnummer
internal fun HendelserDto.lastSekvensnummer() = if (hendelser.isEmpty()) null else hendelser.last().sekvensnummer
internal fun HendelserDto.amountOfHendelserBefore(sekvensnummer: Long) = max(hendelser.indexOfFirst { it.sekvensnummer == sekvensnummer }, 0)