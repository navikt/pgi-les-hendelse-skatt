package no.nav.pgi.skatt.leshendelse

import org.apache.kafka.clients.producer.ProducerRecord

internal class SekvensnummerProducer(kafkaConfig: KafkaConfig) {

    private val sekvensnummerProducer = kafkaConfig.nextSekvensnummerProducer()

    internal fun writeSekvensnummer(sekvensnummer: Long) {
        val record = ProducerRecord(KafkaConfig.NEXT_SEKVENSNUMMER_TOPIC, "sekvensnummer", sekvensnummer.toString())
        sekvensnummerProducer.send(record).get()
    }
}

//TODO skal vi endre sekvensnummer til Long