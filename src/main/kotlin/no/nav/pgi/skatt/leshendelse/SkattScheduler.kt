package no.nav.pgi.skatt.leshendelse

import no.nav.pensjon.samhandling.env.getVal
import org.slf4j.LoggerFactory
import java.util.*

internal const val HOUR_OF_DAY_TO_START_POLLING_SKATT_ENV_KEY = "hour-at-day-to-poll-skatt"
internal const val MINUTES_TO_WAIT_BEFORE_CALLING_SKATT_ENV_KEY = "minutes-to-wait-before-calling-skatt-again"

private const val DEFAULT_HOUR_OF_DAY_TO_START = "0"

internal class SkattScheduler(env: Map<String, String>) {
    private val logger = LoggerFactory.getLogger(SkattScheduler::class.java)
    private val startPollingTime: Int? = env.getVal(HOUR_OF_DAY_TO_START_POLLING_SKATT_ENV_KEY, DEFAULT_HOUR_OF_DAY_TO_START).toInt()
    private val waitMinsBetweenPolls: Double? = env[MINUTES_TO_WAIT_BEFORE_CALLING_SKATT_ENV_KEY]?.toDouble()

    fun wait(startTime: Calendar = Calendar.getInstance()) {
        do {
            Thread.sleep(100L)
        } while (shouldWait(startTime))
        logger.info("SkattScheduler: Waiting stopped")
    }

    internal fun shouldWait(startTime: Calendar): Boolean = !(isStartPollingTime() || exceededWaitInterval(startTime))

    private fun isStartPollingTime() = Calendar.getInstance()[Calendar.HOUR_OF_DAY] == startPollingTime

    private fun exceededWaitInterval(startTime: Calendar): Boolean = (waitMinsBetweenPolls != null) && (Calendar.getInstance().timeInMillis - startTime.timeInMillis) >= 60000 * waitMinsBetweenPolls
}

