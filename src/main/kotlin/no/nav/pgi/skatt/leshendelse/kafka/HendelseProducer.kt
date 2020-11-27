package no.nav.pgi.skatt.leshendelse.kafka

import no.nav.pgi.skatt.leshendelse.skatt.*
import no.nav.samordning.pgi.schema.Hendelse
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.slf4j.LoggerFactory
import java.util.concurrent.Future

private val LOG = LoggerFactory.getLogger(HendelseProducer::class.java)

internal class HendelseProducer(kafkaFactory: KafkaFactory) {

    private val hendelseProducer = kafkaFactory.hendelseProducer()

    internal fun writeHendelser(hendelser: List<HendelseDto>): FailedHendelse? {
        val sentHendelseList = hendelser
                .map { ProducerRecord(PGI_HENDELSE_TOPIC, it.mapToAvroHendelseKey(), it.mapToAvroHendelse()) }
                .map { SentRecord(hendelseProducer.send(it), it.value()) }

        return sentHendelseList.verifyPersisted().also { loggWrittenHendelser(it, hendelser) }
    }

    internal fun close() = hendelseProducer.close().also { LOG.info("closing hendelse hendelseProducer") }

    private fun loggWrittenHendelser(failedHendelse: FailedHendelse?, hendelser: List<HendelseDto>) {
        if (failedHendelse == null) {
            LOG.info("Added ${hendelser.size} hendelser to $PGI_HENDELSE_TOPIC. From sekvensnummer ${hendelser.fistSekvensnummer()} to ${hendelser.lastSekvensnummer()}")
        } else {
            val hendelserAdded = hendelser.amountOfHendelserBefore(failedHendelse.hendelse.getSekvensnummer())
            LOG.info("Failed after adding $hendelserAdded hendelser to $PGI_HENDELSE_TOPIC at sekvensnummer ${failedHendelse.hendelse.getSekvensnummer()}")
        }
    }
}

internal fun List<SentRecord>.verifyPersisted(): FailedHendelse? {
    forEach {
        try {
            it.promise.get()
        } catch (e: Exception) {
            return FailedHendelse(e, it.hendelse)
        }
    }
    return null
}

internal data class SentRecord(internal val promise: Future<RecordMetadata>, internal val hendelse: Hendelse)
internal data class FailedHendelse(internal val exception: Exception, internal val hendelse: Hendelse)