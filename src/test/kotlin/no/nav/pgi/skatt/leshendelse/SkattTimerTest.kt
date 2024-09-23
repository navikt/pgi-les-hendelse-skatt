package no.nav.pgi.skatt.leshendelse

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.math.abs

private const val TWENTY_MILLIS = 20L


internal class SkattTimerTest {

    @Test
    fun `Should not wait if DELAY_IN_SECONDS is set to zero`() {
        val timer = SkattTimer(mapOf(SkattTimer.DELAY_IN_SECONDS_ENV_KEY to "0"))

        val timeBefore = System.currentTimeMillis()
        timer.delay()
        val timeAfter = System.currentTimeMillis()

        assertThat(timeAfter - timeBefore).isLessThan(TWENTY_MILLIS)
    }

    @Test
    fun `Should wait 3 seconds if DELAY_IN_SECONDS is set to three seconds`() {
        val timer = SkattTimer(mapOf(SkattTimer.DELAY_IN_SECONDS_ENV_KEY to "3"))

        val timeBeforePlusThreeSeconds = System.currentTimeMillis() + 3000L
        timer.delay()
        val timeAfter = System.currentTimeMillis()

        assert(differenceBetween(timeBeforePlusThreeSeconds, timeAfter) isLessThen TWENTY_MILLIS)
    }
}

internal fun differenceBetween(timeInMillis1: Long, timeInMillis2: Long): Long =
    abs(timeInMillis1 - timeInMillis2)

internal infix fun Long.isLessThen(errorMargin: Long): Boolean = this < errorMargin