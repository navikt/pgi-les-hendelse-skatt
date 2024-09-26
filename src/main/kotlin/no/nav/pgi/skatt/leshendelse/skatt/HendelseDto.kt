package no.nav.pgi.skatt.leshendelse.skatt

import no.nav.pgi.domain.Hendelse
import no.nav.pgi.domain.HendelseKey
import no.nav.pgi.domain.HendelseMetadata
import no.nav.pgi.skatt.leshendelse.Sekvensnummer
import kotlin.math.max

data class HendelserDtoWrapper(val hendelser: List<HendelseDto> = ArrayList())
data class HendelseDto(val identifikator: String, val gjelderPeriode: String, val sekvensnummer: Long)

internal fun List<HendelseDto>.getNextSekvensnummer() = if (isEmpty()) Sekvensnummer.USE_PREVIOUS else this[size - 1].sekvensnummer + 1
internal fun List<HendelseDto>.firstSekvensnummer() = if (isEmpty()) null else first().sekvensnummer
internal fun List<HendelseDto>.lastSekvensnummer() = if (isEmpty()) null else last().sekvensnummer
internal fun List<HendelseDto>.amountOfHendelserBefore(sekvensnummer: Long) = max(indexOfFirst { it.sekvensnummer == sekvensnummer }, 0)

internal fun HendelseDto.mapToHendelseKey() = HendelseKey(identifikator, gjelderPeriode)
internal fun HendelseDto.mapToHendelse() = Hendelse(sekvensnummer, identifikator, gjelderPeriode, HendelseMetadata(0))