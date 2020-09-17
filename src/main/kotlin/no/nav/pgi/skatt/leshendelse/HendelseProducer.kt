package no.nav.pgi.skatt.leshendelse

import no.nav.pgi.skatt.leshendelse.skatt.Hendelse
import no.nav.pgi.skatt.leshendelse.skatt.getHendelseKey
import org.apache.kafka.clients.producer.ProducerRecord

internal class HendelseProducer(kafkaConfig: KafkaConfig) {
    private val producer = kafkaConfig.hendelseProducer()

    internal fun writeHendelse(hendelse: Hendelse) {
        val record = ProducerRecord(KafkaConfig.PGI_HENDELSE_TOPIC, hendelse.getHendelseKey(), hendelse.toString())
        println("Hendelse ser slik ut:\n\n $hendelse")
        producer.send(record).get()
    }
}