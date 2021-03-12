package no.nav.pgi.skatt.leshendelse.skatt

import no.nav.pgi.skatt.leshendelse.Sekvensnummer
import no.nav.samordning.pgi.schema.Hendelse
import no.nav.samordning.pgi.schema.HendelseKey
import no.nav.samordning.pgi.schema.HendelseMetadata
import kotlin.math.max

data class HendelserDtoWrapper(val hendelser: List<HendelseDto> = ArrayList())
data class HendelseDto(val identifikator: String, val gjelderPeriode: String, val sekvensnummer: Long)

internal fun List<HendelseDto>.getNextSekvensnummer() = if (isEmpty()) Sekvensnummer.USE_PREVIOUS else this[size - 1].sekvensnummer + 1
internal fun List<HendelseDto>.fistSekvensnummer() = if (isEmpty()) null else first().sekvensnummer
internal fun List<HendelseDto>.lastSekvensnummer() = if (isEmpty()) null else last().sekvensnummer
internal fun List<HendelseDto>.amountOfHendelserBefore(sekvensnummer: Long) = max(indexOfFirst { it.sekvensnummer == sekvensnummer }, 0)

internal fun HendelseDto.mapToAvroHendelseKey() = HendelseKey(identifikator, gjelderPeriode)
internal fun HendelseDto.mapToAvroHendelse() = Hendelse(sekvensnummer, identifikator, gjelderPeriode, HendelseMetadata(0))