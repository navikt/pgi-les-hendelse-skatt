package no.nav.pgi.skatt.leshendelse.skatt

data class Hendelser(val nestesekvensnr: Long?, val hendelser: ArrayList<Hendelse>? = ArrayList()) {
    internal fun size() = hendelser?.size
    internal fun validate() {
        if (nestesekvensnr == null) throw GrunnlagPgiHendelserValidationException("Hendelser.nestesekvensnr var null")
    }
}

data class Hendelse(val norskPersonidentifikator: String?, val inntektsaar: String?, val sekvensnr: Long?) {
    fun getHendelseKey(): String = "$norskPersonidentifikator-$inntektsaar"
}

internal class GrunnlagPgiHendelserValidationException(message: String) : Exception(message)


