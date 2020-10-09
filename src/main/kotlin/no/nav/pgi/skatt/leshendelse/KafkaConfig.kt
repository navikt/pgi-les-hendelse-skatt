package no.nav.pgi.skatt.leshendelse

import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.samordning.pgi.schema.Hendelse
import no.nav.samordning.pgi.schema.HendelseKey
import org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig.*
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig.*
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol.SASL_SSL
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import java.lang.Integer.MAX_VALUE

internal const val GROUP_ID = "pgi-sekvensnummer-consumer-group"


internal class KafkaConfig(environment: Map<String, String> = System.getenv()) {
    private val schemaRegistryUrl = environment.getVal(SCHEMA_REGISTRY_ENV_KEY)
    private val bootstrapServers = environment.getVal(BOOTSTRAP_SERVERS_ENV_KEY)
    private val saslMechanism = environment.getVal(SASL_MECHANISM_ENV_KEY, "PLAIN")
    private val securityProtocol = environment.getVal(SECURITY_PROTOCOL_ENV_KEY, SASL_SSL.name)
    private val saslJaasConfig = createSaslJaasConfig(
            environment.getVal(USERNAME_ENV_KEY),
            environment.getVal(PASSWORD_ENV_KEY)
    )

    internal fun nextSekvensnummerProducer() = KafkaProducer<Nothing, String>(
            commonConfig() + sekvensnummerProducerConfig())

    internal fun nextSekvensnummerConsumer() = KafkaConsumer<Nothing, String>(
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

    private fun commonConfig() = mapOf(
            BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            SECURITY_PROTOCOL_CONFIG to securityProtocol,
            SaslConfigs.SASL_MECHANISM to saslMechanism,
            SaslConfigs.SASL_JAAS_CONFIG to saslJaasConfig
    )

    private fun createSaslJaasConfig(username: String, password: String) =
            """org.apache.kafka.common.security.plain.PlainLoginModule required username="$username" password="$password";"""

    companion object {
        const val BOOTSTRAP_SERVERS_ENV_KEY = "KAFKA_BOOTSTRAP_SERVERS"
        const val USERNAME_ENV_KEY = "USERNAME"
        const val PASSWORD_ENV_KEY = "PASSWORD"
        const val SASL_MECHANISM_ENV_KEY = "KAFKA_SASL_MECHANISM"
        const val SECURITY_PROTOCOL_ENV_KEY = "KAFKA_SECURITY_PROTOCOL"
        const val NEXT_SEKVENSNUMMER_TOPIC = "privat-pgi-nextSekvensnummer"
        const val PGI_HENDELSE_TOPIC = "privat-pgi-hendelse"
        const val SCHEMA_REGISTRY_ENV_KEY = "KAFKA_SCHEMA_REGISTRY"
    }
}