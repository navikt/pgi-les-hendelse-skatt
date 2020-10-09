package no.nav.pgi.skatt.leshendelse.skatt

private const val FAULTY_SEKVENSNUMMER = -1L

data class HendelserDto(val nestesekvensnr: Long = FAULTY_SEKVENSNUMMER, val hendelser: List<HendelseDto> = ArrayList()) {
    init {
        if (nestesekvensnr == FAULTY_SEKVENSNUMMER) throw HendelserDtoException("Mangler neste sekvensnummer.")
    }

    internal fun size() = hendelser.size
}

data class HendelseDto(val identifikator: String?, val gjelderPeriode: String?, val sekvensnr: Long?) {
    fun getHendelseKey(): String = "$identifikator-$gjelderPeriode"
}

internal class HendelserDtoException(message: String) : Exception(message)