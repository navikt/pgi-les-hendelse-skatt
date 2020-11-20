package no.nav.pgi.skatt.leshendelse

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

internal class SkattSchedulerTest {

    @Test
    fun `Wait for 6 seconds`(){
        val startTime = now()
        val scheduler = SkattScheduler(mapOf(MINUTES_TO_WAIT_BEFORE_CALLING_SKATT_ENV_KEY to "0.1"))
        scheduler.wait()
        assertTrue(startTime secondsAfter now() in 6..7)
    }

    @Test
    fun `Not wait if hour of day to start is now`() {
        val sameHourOfDay = Calendar.getInstance()[Calendar.HOUR_OF_DAY]

        val scheduler = SkattScheduler(mapOf(HOUR_OF_DAY_TO_START_POLLING_SKATT_ENV_KEY to sameHourOfDay.toString()))
        assertFalse(scheduler.shouldWait(Calendar.getInstance()))
    }

    @Test
    fun `Not wait if wait-interval is passed`() {
        val scheduler = SkattScheduler(mapOf(MINUTES_TO_WAIT_BEFORE_CALLING_SKATT_ENV_KEY to "2"))
        val startTime = Calendar.getInstance().apply {
            add(Calendar.MINUTE, -2)
        }
        assertFalse(scheduler.shouldWait(startTime))
    }

    @Test
    fun `wait-interval should have precedence over hour of day to start`() {
        val minusOneHour = Calendar.getInstance()[Calendar.HOUR_OF_DAY] - 1
        val scheduler = SkattScheduler(
                mapOf(
                        MINUTES_TO_WAIT_BEFORE_CALLING_SKATT_ENV_KEY to "2",
                        HOUR_OF_DAY_TO_START_POLLING_SKATT_ENV_KEY to minusOneHour.toString()
                ))
        val startTime = Calendar.getInstance().apply {
            add(Calendar.MINUTE, -2)
        }
        assertFalse(scheduler.shouldWait(startTime))
    }


    @Test
    fun `Wait if wait-interval is two minutes ahead of current time `() {
        val scheduler = SkattScheduler(mapOf(MINUTES_TO_WAIT_BEFORE_CALLING_SKATT_ENV_KEY to "2"))
        val startTime = Calendar.getInstance()
        assertTrue(scheduler.shouldWait(startTime))
    }

    @Test
    fun `Wait if wait-interval is not set and hour of day to start is later`() {
        val plusTwoHours = Calendar.getInstance()[Calendar.HOUR_OF_DAY] + 2

        val scheduler = SkattScheduler(mapOf(HOUR_OF_DAY_TO_START_POLLING_SKATT_ENV_KEY to plusTwoHours.toString()))
        assertTrue(scheduler.shouldWait(Calendar.getInstance()))
    }

    @Test
    fun `Wait if wait-interval is not set and hour of day to start is was earlier`() {
        val minusOneHour = Calendar.getInstance()[Calendar.HOUR_OF_DAY] - 1

        val scheduler = SkattScheduler(mapOf(HOUR_OF_DAY_TO_START_POLLING_SKATT_ENV_KEY to minusOneHour.toString()))
        assertTrue(scheduler.shouldWait(Calendar.getInstance()))
    }
}

private fun now() = Calendar.getInstance()
private infix fun Calendar.secondsAfter(other: Calendar): Long = (other.timeInMillis - this.timeInMillis) / 1000