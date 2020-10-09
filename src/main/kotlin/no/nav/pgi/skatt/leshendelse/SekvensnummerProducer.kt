package no.nav.pgi.skatt.leshendelse

import org.apache.kafka.clients.producer.ProducerRecord

internal class SekvensnummerProducer(kafkaConfig: KafkaConfig) {

    private val sekvensnummerProducer = kafkaConfig.nextSekvensnummerProducer()

    internal fun writeSekvensnummer(sekvensnummer: String) {
        val record = ProducerRecord(KafkaConfig.NEXT_SEKVENSNUMMER_TOPIC, "sekvensnummer", sekvensnummer)
        sekvensnummerProducer.send(record).get()
    }
}