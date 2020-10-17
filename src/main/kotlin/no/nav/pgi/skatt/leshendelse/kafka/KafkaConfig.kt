package no.nav.pgi.skatt.leshendelse.kafka

import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.pensjon.samhandling.env.getVal
import no.nav.samordning.pgi.schema.Hendelse
import no.nav.samordning.pgi.schema.HendelseKey
import org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig.*
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig.*
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import java.lang.Integer.MAX_VALUE

internal const val GROUP_ID = "pgi-sekvensnummer-consumer-group"
const val NEXT_SEKVENSNUMMER_TOPIC = "pensjonsamhandling.privat-pgi-nextsekvensnummer"
const val PGI_HENDELSE_TOPIC = "pensjonsamhandling.privat-pgi-hendelse"

internal class KafkaConfig(environment: Map<String, String> = System.getenv(), private val securityStrategy: SecurityStrategy = SslStrategy()) {
    private val schemaRegistryUrl = environment.getVal(SCHEMA_REGISTRY)
    private val bootstrapServers = environment.getVal(BOOTSTRAP_SERVERS)

    internal fun nextSekvensnummerProducer() = KafkaProducer<String, String>(
            commonConfig() + sekvensnummerProducerConfig())

    internal fun nextSekvensnummerConsumer() = KafkaConsumer<String, String>(
            commonConfig() + sekvensnummerConsumerConfig())

    internal fun hendelseProducer() = KafkaProducer<HendelseKey, Hendelse>(
            commonConfig() + hendelseProducerConfig())

    private fun sekvensnummerConsumerConfig() = mapOf(
            KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            GROUP_ID_CONFIG to GROUP_ID,
            ENABLE_AUTO_COMMIT_CONFIG to false,
            AUTO_OFFSET_RESET_CONFIG to "earliest"
    )

    private fun sekvensnummerProducerConfig() = mapOf(
            KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ACKS_CONFIG to "all",
            RETRIES_CONFIG to MAX_VALUE
    )
    
    private fun hendelseProducerConfig() = mapOf(
            "schema.registry.url" to schemaRegistryUrl,
            KEY_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java,
            VALUE_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java,
            ACKS_CONFIG to "all",
            RETRIES_CONFIG to MAX_VALUE
    )

    private fun commonConfig() = mapOf(BOOTSTRAP_SERVERS_CONFIG to bootstrapServers) + securityStrategy.securityConfig()

    internal interface SecurityStrategy {
        fun securityConfig(): Map<String, String>
    }

    internal companion object EnvironmentKeys {
        const val BOOTSTRAP_SERVERS = "KAFKA_BOOTSTRAP_SERVERS"
        const val SCHEMA_REGISTRY = "KAFKA_SCHEMA_REGISTRY"
    }
}