package no.nav.pgi.skatt.leshendelse

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong

class Counters(private val meterRegistry: MeterRegistry) {

    private val persistedSekvensnummer = AtomicLong(0)

    private val hendelserToTopic = meterRegistry.counter(
        "pgi_hendelser_added_to_topic",
        listOf(Tag.of("help", "AAntall hendelser lagt til topic"))
    )

    private val hendelserFailedToTopic =
        meterRegistry.counter(
            "pgi_hendelser_failed_to_topic",
            listOf(
                Tag.of("help", "Antall hendelser som feilet når de skulle legges til topic eller vil bli overskrevet")
            )
        )

    private val polledFromSkattCounter = meterRegistry.counter(
        "pgi_hendelser_polled_from_skatt",
        listOf(Tag.of("help", "Antall hendelser hentet fra skatt"))
    )

    private val persistedSekvensnummerGauge = meterRegistry.gauge(
        "persistedSekvensnummer",
        listOf(Tag.of("help", "Siste persisterte som brukes når det hentes pgi-hendelser fra skatt")),
        persistedSekvensnummer
    )

    fun incrementHendelserTopTopic(count: Int) {
        log.info("Counters.incrementHendelserTopTopic:$count")
        hendelserToTopic.increment(count.toDouble())
    }

    fun incrementFailedToTopic(count: Int) {
        log.info("Counters.incrementFailedToTopic:$count")
        hendelserFailedToTopic.increment(count.toDouble())
    }

    fun incrementPolledFromSkatt(count: Int) {
        log.info("Counters.incrementPolledFromSkattTopic:$count")
        polledFromSkattCounter.increment(count.toDouble())
    }

    fun setPersistredSekvensnummer(sekvensnummer: Long) {
        log.info("Counters.setPersistredSekvensnummer:$sekvensnummer")
        persistedSekvensnummer.set(sekvensnummer)
    }

    companion object {
        private val log = LoggerFactory.getLogger(Counters::class.java)
    }

}