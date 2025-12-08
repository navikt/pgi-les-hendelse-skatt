package no.nav.pgi.skatt.leshendelse.kafka

import net.logstash.logback.marker.Markers
import no.nav.pgi.domain.Hendelse
import no.nav.pgi.domain.serialization.PgiDomainSerializer
import no.nav.pgi.skatt.leshendelse.Counters
import no.nav.pgi.skatt.leshendelse.skatt.HendelseDto
import no.nav.pgi.skatt.leshendelse.skatt.amountOfHendelserBefore
import no.nav.pgi.skatt.leshendelse.skatt.firstSekvensnummer
import no.nav.pgi.skatt.leshendelse.skatt.lastSekvensnummer
import no.nav.pgi.skatt.leshendelse.skatt.mapToHendelse
import no.nav.pgi.skatt.leshendelse.skatt.mapToHendelseKey
import no.nav.pgi.skatt.leshendelse.util.maskFnr
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.slf4j.LoggerFactory
import java.util.concurrent.Future

internal class HendelseProducer(
    val counters: Counters,
    val hendelseProducer: Producer<String, String>
) {

    internal fun writeHendelser(hendelser: List<HendelseDto>): FailedHendelse? {
        try {
            val sendtRecords = hendelser
                .map { createRecord(it) }
                .map { sendRecord(it) }
            return sendtRecords.verifyWritten().also { loggWrittenHendelser(it, hendelser) }
        } catch (e: Throwable) {
            throw HendelseProducerException("Feil ved skriving til kafka", e)
        }
    }

    internal fun close() {
        LOG.info("closing hendelse hendelseProducer")
        hendelseProducer.close()
    }

    private fun createRecord(hendelse: HendelseDto): ProducerRecord<String, String> {
        val key = PgiDomainSerializer().toJson(hendelse.mapToHendelseKey())
        val value = PgiDomainSerializer().toJson(hendelse.mapToHendelse())
        return ProducerRecord(PGI_HENDELSE_TOPIC, key, value)
    }

    private fun sendRecord(record: ProducerRecord<String, String>) =
        SentRecord(hendelseProducer.send(record), record.value())

    private fun loggWrittenHendelser(failedHendelse: FailedHendelse?, hendelser: List<HendelseDto>) {
        if (failedHendelse == null) {
            counters.incrementHendelserTopTopic(hendelser.size)
            if (hendelser.isNotEmpty()) {
                LOG.info("Added ${hendelser.size} hendelser to $PGI_HENDELSE_TOPIC. From sekvensnummer ${hendelser.firstSekvensnummer()} to ${hendelser.lastSekvensnummer()}")

                for (hendelse in hendelser) {
                    val sekvensnummer = hendelse.sekvensnummer
                    val marker = Markers.append("sekvensnummer", sekvensnummer.toString())
                    LOG.info(
                        marker,
                        "Added hendelse: ${hendelse.mapToHendelse().toString().maskFnr()}. Sekvensnummer: $sekvensnummer"
                    )
                    SECURE_LOG.info(
                        marker,
                        "Added hendelse: ${hendelse.mapToHendelse()}. Sekvensnummer: $sekvensnummer"
                    )
                }
            }
        } else {
            val hendelserAdded = hendelser.amountOfHendelserBefore(failedHendelse.hendelse.sekvensnummer)
            counters.incrementHendelserTopTopic(hendelserAdded)
            counters.incrementFailedToTopic(hendelser.size - hendelserAdded)
            LOG.info("Failed after adding $hendelserAdded hendelser to $PGI_HENDELSE_TOPIC at sekvensnummer ${failedHendelse.hendelse.sekvensnummer}")
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(HendelseProducer::class.java)
        private val SECURE_LOG = LoggerFactory.getLogger("team")
    }
}

internal fun List<SentRecord>.verifyWritten(): FailedHendelse? {
    forEach {
        try {
            it.future.get()
        } catch (e: Exception) {
            val hendelse = PgiDomainSerializer().fromJson(Hendelse::class, it.hendelse)
            return FailedHendelse(HendelseProducerException("Feil ved henting av resultat fra kafka", e), hendelse)
        }
    }
    return null
}

internal data class SentRecord(internal val future: Future<RecordMetadata>, internal val hendelse: String)

internal data class FailedHendelse(internal val exception: Exception, internal val hendelse: Hendelse)

class HendelseProducerException(msg: String, e: Throwable?) : RuntimeException(msg, e) {
    constructor(msg: String) : this(msg, null)
}