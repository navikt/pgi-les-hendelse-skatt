package no.nav.pgi.skatt.leshendelse.kafka

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.producer.Producer


internal interface KafkaFactory {
    fun nextSekvensnummerProducer(): Producer<String, String>

    fun nextSekvensnummerConsumer(): Consumer<String, String>

    fun hendelseProducer(): Producer<String, String>
}