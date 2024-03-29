package no.nav.pgi.skatt.leshendelse.kafka

import no.nav.samordning.pgi.schema.Hendelse
import no.nav.samordning.pgi.schema.HendelseKey
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer


internal class KafkaHendelseFactory(private val kafkaConfig: KafkaConfig = KafkaConfig()) : KafkaFactory {
    override fun nextSekvensnummerProducer() = KafkaProducer<String, String>(
        kafkaConfig.commonConfig() + kafkaConfig.sekvensnummerProducerConfig()
    )

    override fun nextSekvensnummerConsumer() = KafkaConsumer<String, String>(
        kafkaConfig.commonConfig() + kafkaConfig.sekvensnummerConsumerConfig()
    )

    override fun hendelseProducer() = KafkaProducer<HendelseKey, Hendelse>(
        kafkaConfig.commonConfig() + kafkaConfig.schemaRegistryConfig() + kafkaConfig.hendelseProducerConfig()
    )
}

internal interface KafkaFactory {
    fun nextSekvensnummerProducer(): Producer<String, String>

    fun nextSekvensnummerConsumer(): Consumer<String, String>

    fun hendelseProducer(): Producer<HendelseKey, Hendelse>
}