package no.nav.pgi.skatt.leshendelse

import no.nav.common.KafkaEnvironment
import org.apache.kafka.clients.CommonClientConfigs

import org.apache.kafka.clients.consumer.ConsumerConfig.*
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration

const val KAFKA_TEST_USERNAME = "srvTest"
const val KAFKA_TEST_PASSWORD = "opensourcedPassword"

class KafkaTestEnvironment {

    private val kafkaTestEnvironment: KafkaEnvironment = KafkaEnvironment(topicNames = listOf(KafkaConfig.NEXT_SEKVENSNUMMER_TOPIC, KafkaConfig.PGI_HENDELSE_TOPIC))
    internal val hendelseTestConsumer = hendelseTestConsumer()

    init {
        kafkaTestEnvironment.start()
        hendelseTestConsumer.subscribe(listOf(KafkaConfig.PGI_HENDELSE_TOPIC))
    }

    internal fun tearDown() = kafkaTestEnvironment.tearDown()

    internal fun kafkaEnvVariables() = mapOf<String, String>(
            KafkaConfig.BOOTSTRAP_SERVERS_ENV_KEY to kafkaTestEnvironment.brokersURL,
            KafkaConfig.USERNAME_ENV_KEY to KAFKA_TEST_USERNAME,
            KafkaConfig.PASSWORD_ENV_KEY to KAFKA_TEST_PASSWORD,
            KafkaConfig.SECURITY_PROTOCOL_ENV_KEY to SecurityProtocol.PLAINTEXT.name)

    private fun hendelseTestConsumer() = KafkaConsumer<String, String>(
            mapOf(
                    CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to kafkaTestEnvironment.brokersURL,
                    KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                    VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                    GROUP_ID_CONFIG to "LOL",
                    AUTO_OFFSET_RESET_CONFIG to "earliest",
                    ENABLE_AUTO_COMMIT_CONFIG to false
            )
    )

    //Duration 4 seconds to allow for hendelse to be added to topic
    fun consumeHendelseTopic(): List<ConsumerRecord<String, String>> = hendelseTestConsumer.poll(Duration.ofSeconds(4)).records(KafkaConfig.PGI_HENDELSE_TOPIC).toList()

    fun getFirstRecordOnTopic() = consumeHendelseTopic()[0]
}