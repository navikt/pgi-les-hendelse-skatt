package no.nav.pgi.skatt.leshendelse.kafka

import no.nav.pgi.skatt.leshendelse.skatt.HendelseDto
import no.nav.pgi.skatt.leshendelse.skatt.HendelserDto
import no.nav.samordning.pgi.schema.Hendelse
import no.nav.samordning.pgi.schema.HendelseKey
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

private val LOGGER = LoggerFactory.getLogger(HendelseProducer::class.java)

internal class HendelseProducer(kafkaConfig: KafkaConfig) {

    private val producer = kafkaConfig.hendelseProducer()

    private fun writeHendelse(hendelseDto: HendelseDto) {
        val record = ProducerRecord(PGI_HENDELSE_TOPIC, hendelseDto.mapToHendelseKey(), hendelseDto.mapToHendelse())
        producer.send(record).get()
    }

    internal fun writeHendelser(hendelserDto: HendelserDto) {
        hendelserDto.hendelser.forEach { writeHendelse(it) }
        LOGGER.info("Added ${hendelserDto.hendelser.size} hendelser to $PGI_HENDELSE_TOPIC. ")
    }
}

internal fun HendelseDto.mapToHendelseKey() = HendelseKey(identifikator, gjelderPeriode)
internal fun HendelseDto.mapToHendelse() = Hendelse(sekvensnummer, identifikator, gjelderPeriode)