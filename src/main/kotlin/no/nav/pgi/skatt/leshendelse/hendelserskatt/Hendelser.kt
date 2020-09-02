package no.nav.pgi.skatt.leshendelse.hendelserskatt

const val FAULTY_SEKVENSNUMMER = -1

data class Hendelser(var hendelser: ArrayList<Hendelse> = ArrayList());
data class Hendelse(var sekvensnummer: Int = FAULTY_SEKVENSNUMMER, var identifikator: String = "", var gjelderPeriode: String = "")

internal fun Hendelser.validate() = hendelser.forEach { it.validate() }

internal fun Hendelse.validate() {

}