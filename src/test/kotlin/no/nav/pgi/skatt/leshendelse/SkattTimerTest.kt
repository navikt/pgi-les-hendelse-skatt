package no.nav.pgi.skatt.leshendelse

import org.junit.jupiter.api.Test
import java.util.*
import kotlin.math.abs

private const val TWO_HUNDREDTH_OF_A_SECOND = 20L


internal class SkattTimerTest {

    @Test
    fun `Should not wait if DELAY_IN_SECONDS is set to zero`() {
        val timer = SkattTimer(mapOf(SkattTimer.DELAY_IN_SECONDS_ENV_KEY to "0"))

        val timeBefore = Calendar.getInstance()
        timer.delay()
        val timeAfter = Calendar.getInstance()

        assert(differenceBetween(timeBefore, timeAfter) isLessThen TWO_HUNDREDTH_OF_A_SECOND)
    }

    @Test
    fun `Should wait 3 seconds if DELAY_IN_SECONDS is set to three seconds`() {
        val timer = SkattTimer(mapOf(SkattTimer.DELAY_IN_SECONDS_ENV_KEY to "3"))

        val timeBeforePlusThreeSeconds = Calendar.getInstance().also { it.add(Calendar.SECOND, 3) }
        timer.delay()
        val timeAfter = Calendar.getInstance()

        assert(differenceBetween(timeBeforePlusThreeSeconds, timeAfter) isLessThen TWO_HUNDREDTH_OF_A_SECOND)
    }
}

internal fun differenceBetween(calendar1: Calendar, calendar2: Calendar): Long =
    abs(calendar1.timeInMillis - calendar2.timeInMillis)

internal infix fun Long.isLessThen(errorMargin: Long): Boolean = this < errorMargin