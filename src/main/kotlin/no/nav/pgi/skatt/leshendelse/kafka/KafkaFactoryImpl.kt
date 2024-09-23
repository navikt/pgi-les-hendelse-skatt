package no.nav.pgi.skatt.leshendelse.kafka

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer

internal class KafkaFactoryImpl(private val kafkaConfig: KafkaConfig = KafkaConfig()) : KafkaFactory {
    override fun nextSekvensnummerProducer() = KafkaProducer<String, String>(
        kafkaConfig.commonConfig() + kafkaConfig.sekvensnummerProducerConfig()
    )

    override fun nextSekvensnummerConsumer() = KafkaConsumer<String, String>(
        kafkaConfig.commonConfig() + kafkaConfig.sekvensnummerConsumerConfig()
    )

    override fun hendelseProducer() = KafkaProducer<String, String>(
        kafkaConfig.commonConfig() + kafkaConfig.hendelseProducerConfig()
    )
}