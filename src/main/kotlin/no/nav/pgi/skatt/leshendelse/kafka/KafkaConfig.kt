package no.nav.pgi.skatt.leshendelse.kafka

import no.nav.pgi.skatt.leshendelse.util.getVal
import org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig.*
import org.apache.kafka.clients.producer.ProducerConfig.*
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import java.lang.Integer.MAX_VALUE

internal const val GROUP_ID = "pgi-sekvensnummer-consumer-group"
const val NEXT_SEKVENSNUMMER_TOPIC = "pensjonopptjening.privat-pgi-nextsekvensnummer"
const val PGI_HENDELSE_TOPIC = "pensjonopptjening.privat-pgi-hendelse"

internal class KafkaConfig(
    environment: Map<String, String> = System.getenv(),
    private val securityStrategy: SecurityStrategy = SslStrategy()
) {
    private val bootstrapServers = environment.getVal(BOOTSTRAP_SERVERS)

    internal fun sekvensnummerConsumerConfig() = mapOf(
        KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        GROUP_ID_CONFIG to GROUP_ID,
        ENABLE_AUTO_COMMIT_CONFIG to false,
        AUTO_OFFSET_RESET_CONFIG to "earliest"
    )

    internal fun sekvensnummerProducerConfig() = mapOf(
        KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
        VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
        ACKS_CONFIG to "all",
        RETRIES_CONFIG to MAX_VALUE
    )

    internal fun hendelseProducerConfig() = mapOf(
        KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
        VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
        ACKS_CONFIG to "all",
        RETRIES_CONFIG to MAX_VALUE
    )

    internal fun commonConfig() =
        mapOf(BOOTSTRAP_SERVERS_CONFIG to bootstrapServers) + securityStrategy.securityConfig()

    internal interface SecurityStrategy {
        fun securityConfig(): Map<String, String>
    }

    internal companion object EnvironmentKeys {
        const val BOOTSTRAP_SERVERS = "KAFKA_BROKERS"
    }
}