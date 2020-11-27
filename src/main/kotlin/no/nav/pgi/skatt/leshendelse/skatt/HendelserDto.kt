package no.nav.pgi.skatt.leshendelse.skatt

import no.nav.pgi.skatt.leshendelse.Sekvensnummer

data class HendelserDto(val hendelser: List<HendelseDto> = ArrayList())
data class HendelseDto(val identifikator: String, val gjelderPeriode: String, val sekvensnummer: Long)

internal fun HendelserDto.getNextSekvensnummer() = if (hendelser.isEmpty()) Sekvensnummer.USE_PREVIOUS else hendelser[hendelser.size - 1].sekvensnummer + 1
internal fun HendelserDto.size() = hendelser.size
internal fun HendelserDto.fistSekvensnummer() = if(hendelser.isEmpty()) null else hendelser.first().sekvensnummer
internal fun HendelserDto.lastSekvensnummer() = if(hendelser.isEmpty()) null else hendelser.last().sekvensnummer
internal fun HendelserDto.hendelserBefore(sekvensnummer: Long) = hendelser.indexOfFirst { it.sekvensnummer == sekvensnummer }

internal fun HendelseDto.getHendelseKey(): String = "$identifikator-$gjelderPeriode"
internal operator fun HendelseDto.compareTo(other: HendelseDto): Int = if (sekvensnummer == other.sekvensnummer) 0 else if (sekvensnummer > other.sekvensnummer) 1 else -1
