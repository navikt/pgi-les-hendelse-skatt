package no.nav.pgi.skatt.leshendelse.kafka

import no.nav.pgi.skatt.leshendelse.skatt.HendelseDto
import no.nav.pgi.skatt.leshendelse.skatt.HendelserDto
import no.nav.samordning.pgi.schema.Hendelse
import no.nav.samordning.pgi.schema.HendelseKey
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.slf4j.LoggerFactory
import java.util.concurrent.Future

private val LOG = LoggerFactory.getLogger(HendelseProducer::class.java)

internal class HendelseProducer(kafkaFactory: KafkaFactory) {

    private val producer = kafkaFactory.hendelseProducer()

    internal fun writeHendelser(hendelserDto: HendelserDto): FailedHendelse? {
        val sentHendelseList = hendelserDto.hendelser
                .map { ProducerRecord(PGI_HENDELSE_TOPIC, it.mapToHendelseKey(), it.mapToHendelse()) }
                .map { SentRecord(producer.send(it), it.value()) }

        return sentHendelseList.verifyPersisted()
                .also { loggWrittenHendelser(it, hendelserDto) }
    }

    internal fun close() = producer.close()

    private fun loggWrittenHendelser(failedHendelse: FailedHendelse?, hendelserDto: HendelserDto) {
        if (failedHendelse == null) {
            LOG.info("Added ${hendelserDto.hendelser.size} hendelser to $PGI_HENDELSE_TOPIC")
        } else {
            LOG.info("Failed while adding hendelse to $PGI_HENDELSE_TOPIC at sekvensnummer ${failedHendelse.hendelse.getSekvensnummer()}")
        }
    }
}

internal fun HendelseDto.mapToHendelseKey() = HendelseKey(identifikator, gjelderPeriode)
internal fun HendelseDto.mapToHendelse() = Hendelse(sekvensnummer, identifikator, gjelderPeriode)
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