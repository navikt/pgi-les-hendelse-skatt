package no.nav.pgi.skatt.leshendelse.kafka

import io.prometheus.client.Counter
import no.nav.pgi.domain.Hendelse
import no.nav.pgi.domain.serialization.PgiDomainSerializer
import no.nav.pgi.skatt.leshendelse.skatt.*
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.slf4j.LoggerFactory
import java.util.concurrent.Future

private val addedToTopicCounter =
    Counter.build("pgi_hendelser_added_to_topic", "Antall hendelser lagt til topic").register()
private val failToAddToTopicCounter = Counter.build(
    "pgi_hendelser_failed_to_topic",
    "Antall hendelser som feilet når de skulle legges til topic eller vil bli overskrevet"
).register()

internal class HendelseProducer(kafkaFactory: KafkaFactory) {

    private val LOG = LoggerFactory.getLogger(HendelseProducer::class.java)
    private val hendelseProducer = kafkaFactory.hendelseProducer()

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

    internal fun close() = hendelseProducer.close().also { LOG.info("closing hendelse hendelseProducer") }

    private fun createRecord(hendelse: HendelseDto): ProducerRecord<String, String> {
        val key = PgiDomainSerializer().toJson(hendelse.mapToHendelseKey())
        val value = PgiDomainSerializer().toJson(hendelse.mapToHendelse())
        return ProducerRecord(PGI_HENDELSE_TOPIC, key, value)
    }

    private fun sendRecord(record: ProducerRecord<String, String>) =
        SentRecord(hendelseProducer.send(record), record.value())

    private fun loggWrittenHendelser(failedHendelse: FailedHendelse?, hendelser: List<HendelseDto>) {
        if (failedHendelse == null) {
            addedToTopicCounter.inc(hendelser.size.toDouble())
            if (hendelser.isNotEmpty()) {
                LOG.info("Added ${hendelser.size} hendelser to $PGI_HENDELSE_TOPIC. From sekvensnummer ${hendelser.fistSekvensnummer()} to ${hendelser.lastSekvensnummer()}")
            }
        } else {
            val hendelserAdded = hendelser.amountOfHendelserBefore(failedHendelse.hendelse.sekvensnummer)
            addedToTopicCounter.inc(hendelserAdded.toDouble())
            failToAddToTopicCounter.inc((hendelser.size - hendelserAdded).toDouble())
            LOG.info("Failed after adding $hendelserAdded hendelser to $PGI_HENDELSE_TOPIC at sekvensnummer ${failedHendelse.hendelse.sekvensnummer}")
        }
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