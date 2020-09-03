package no.nav.pgi.skatt.leshendelse

import no.nav.common.KafkaEnvironment
import org.apache.kafka.common.security.auth.SecurityProtocol

const val KAFKA_TEST_USERNAME = "srvTest"
const val KAFKA_TEST_PASSWORD = "opensourcedPassword"

class KafkaTestEnvironment {

    private val kafkaTestEnvironment: KafkaEnvironment = KafkaEnvironment(topicNames = listOf(KafkaConfig.NEXT_SEKVENSNUMMER_TOPIC, KafkaConfig.PGI_HENDELSE_TOPIC))

    init {
        kafkaTestEnvironment.start()
    }

    internal fun tearDown() = kafkaTestEnvironment.tearDown()

    internal fun kafkaEnvVariables() = mapOf<String, String>(
            KafkaConfig.BOOTSTRAP_SERVERS_ENV_KEY to kafkaTestEnvironment.brokersURL,
            KafkaConfig.USERNAME_ENV_KEY to KAFKA_TEST_USERNAME,
            KafkaConfig.PASSWORD_ENV_KEY to KAFKA_TEST_PASSWORD,
            KafkaConfig.SECURITY_PROTOCOL_ENV_KEY to SecurityProtocol.PLAINTEXT.name)
}