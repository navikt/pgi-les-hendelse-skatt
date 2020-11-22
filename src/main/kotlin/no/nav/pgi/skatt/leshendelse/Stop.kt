package no.nav.pgi.skatt.leshendelse

open class Stop {
    private var stopped = false

    internal fun stop() {
        stopped = true
        onStop()
    }

    internal open fun onStop(){}

    internal fun isStopped() = stopped

    internal fun isNotStopped() = !stopped
}