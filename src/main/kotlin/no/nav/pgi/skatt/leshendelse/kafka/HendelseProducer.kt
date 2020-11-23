package no.nav.pgi.skatt.leshendelse.kafka

import no.nav.pgi.skatt.leshendelse.skatt.HendelseDto
import no.nav.pgi.skatt.leshendelse.skatt.HendelserDto
import no.nav.samordning.pgi.schema.Hendelse
import no.nav.samordning.pgi.schema.HendelseKey
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

private val LOG = LoggerFactory.getLogger(HendelseProducer::class.java)

internal class HendelseProducer(kafkaConfig: KafkaConfig) {

    private val producer = kafkaConfig.hendelseProducer()

    internal fun writeHendelser(hendelserDto: HendelserDto) {
        hendelserDto.hendelser
                .map { ProducerRecord(PGI_HENDELSE_TOPIC, it.mapToHendelseKey(), it.mapToHendelse()) }
                .forEach { producer.send(it).get() }
        loggWrittenHendelser(hendelserDto)
    }

    internal fun close() = producer.close()

    private fun loggWrittenHendelser(hendelserDto: HendelserDto){
        LOG.info("Added ${hendelserDto.hendelser.size} hendelser to $PGI_HENDELSE_TOPIC")
    }
}

internal fun HendelseDto.mapToHendelseKey() = HendelseKey(identifikator, gjelderPeriode)
internal fun HendelseDto.mapToHendelse() = Hendelse(sekvensnummer, identifikator, gjelderPeriode)