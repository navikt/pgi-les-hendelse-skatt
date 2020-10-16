package no.nav.pgi.skatt.leshendelse.kafka

import no.nav.pgi.skatt.leshendelse.skatt.HendelseDto
import no.nav.pgi.skatt.leshendelse.skatt.HendelserDto
import no.nav.samordning.pgi.schema.Hendelse
import no.nav.samordning.pgi.schema.HendelseKey
import org.apache.kafka.clients.producer.ProducerRecord

internal class HendelseProducer(kafkaConfig: KafkaConfig) {
    private val producer = kafkaConfig.hendelseProducer()

    internal fun writeHendelse(hendelseDto: HendelseDto) {
        val record = ProducerRecord(KafkaConfig.PGI_HENDELSE_TOPIC, hendelseDto.mapToHendelseKey(), hendelseDto.mapToHendelse())
        producer.send(record).get()
    }

    internal fun writeHendelser(hendelserDto: HendelserDto) {
        hendelserDto.hendelser.forEach { writeHendelse(it) }
    }

    internal fun close() = producer.close()
}

internal fun HendelseDto.mapToHendelseKey() = HendelseKey(identifikator, gjelderPeriode)
internal fun HendelseDto.mapToHendelse() = Hendelse(sekvensnr, identifikator, gjelderPeriode)