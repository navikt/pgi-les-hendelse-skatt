package no.nav.pgi.skatt.leshendelse.util

private val fnrRegex = "(\\d{6})\\d{5}".toRegex()

fun String.maskFnr() = fnrRegex.replace(this, "\$1*****")