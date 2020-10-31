package no.nav.pgi.skatt.leshendelse.skatt

internal const val USE_PREVIOUS_SEKVENSNUMMER = -1L

data class HendelserDto(val hendelser: List<HendelseDto> = ArrayList())
data class HendelseDto(val identifikator: String?, val gjelderPeriode: String?, val sekvensnummer: Long)

internal fun HendelserDto.getNesteSekvensnummer() = if (hendelser.isEmpty()) USE_PREVIOUS_SEKVENSNUMMER else hendelser[hendelser.size - 1].sekvensnummer + 1
internal fun HendelserDto.size() = hendelser.size

internal fun HendelseDto.getHendelseKey(): String = "$identifikator-$gjelderPeriode"
internal operator fun HendelseDto.compareTo(other: HendelseDto): Int = if (sekvensnummer == other.sekvensnummer) 0 else if (sekvensnummer > other.sekvensnummer) 1 else -1
