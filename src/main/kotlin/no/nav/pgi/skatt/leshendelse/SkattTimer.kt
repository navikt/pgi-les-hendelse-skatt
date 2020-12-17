package no.nav.pgi.skatt.leshendelse

import no.nav.pensjon.samhandling.env.getVal
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue


internal class SkattTimer(env: Map<String, String>) {
    private val LOG = LoggerFactory.getLogger(SkattTimer::class.java.simpleName)
    private val secondsDelay: Long = env.getVal(DELAY_IN_SECONDS_ENV_KEY, DEFAULT_DELAY).toLong().absoluteValue
    private var closed = false

    fun delay() {
        for (i in 1..secondsDelay) if (closed) break else TimeUnit.SECONDS.sleep(1)
    }

    internal fun close() {
        closed = true
        LOG.info("closing SkattTimer")
    }

    companion object {
        internal const val DELAY_IN_SECONDS_ENV_KEY = "DELAY-IN_SECONDS_BEFORE-POLLING-SKATT"
        private const val DEFAULT_DELAY = "10"
    }
}
