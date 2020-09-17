package no.nav.pgi.skatt.leshendelse

import org.apache.kafka.clients.producer.ProducerRecord

internal class HendelseProducer(kafkaConfig: KafkaConfig) {
    private val producer = kafkaConfig.hendelseProducer()

    internal fun writeHendelse(hendelseKey: String, hendelse: String) {
        val record = ProducerRecord(KafkaConfig.PGI_HENDELSE_TOPIC, hendelseKey, hendelse)
        producer.send(record).get()
    }
}