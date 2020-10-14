package no.nav.pgi.skatt.leshendelse

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG
import no.nav.common.KafkaEnvironment
import no.nav.common.KafkaEnvironment.TopicInfo
import no.nav.samordning.pgi.schema.Hendelse
import no.nav.samordning.pgi.schema.HendelseKey
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig.*
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.security.auth.SecurityProtocol
import java.time.Duration

const val KAFKA_TEST_USERNAME = "srvTest"
const val KAFKA_TEST_PASSWORD = "opensourcedPassword"

class KafkaTestEnvironment {

    private val kafkaTestEnvironment: KafkaEnvironment = KafkaEnvironment(
            withSchemaRegistry = true,
            topicInfos = listOf(
                    TopicInfo(KafkaConfig.NEXT_SEKVENSNUMMER_TOPIC, partitions = 1),
                    TopicInfo(KafkaConfig.PGI_HENDELSE_TOPIC)
            )
    )
    private var hendelseTestConsumer: KafkaConsumer<HendelseKey, Hendelse> = hendelseTestConsumer()

    init {
        kafkaTestEnvironment.start()
        hendelseTestConsumer.subscribe(listOf(KafkaConfig.PGI_HENDELSE_TOPIC))
    }

    internal fun tearDown() = kafkaTestEnvironment.tearDown()

    internal fun kafkaTestEnvironmentVariables() = mapOf<String, String>(
            KafkaConfig.BOOTSTRAP_SERVERS_ENV_KEY to kafkaTestEnvironment.brokersURL,
            KafkaConfig.SCHEMA_REGISTRY_ENV_KEY to kafkaTestEnvironment.schemaRegistry!!.url,
            KafkaConfig.USERNAME_ENV_KEY to KAFKA_TEST_USERNAME,
            KafkaConfig.PASSWORD_ENV_KEY to KAFKA_TEST_PASSWORD,
            KafkaConfig.SECURITY_PROTOCOL_ENV_KEY to SecurityProtocol.PLAINTEXT.name
    )

    private fun hendelseTestConsumer() = KafkaConsumer<HendelseKey, Hendelse>(
            mapOf(
                    CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to kafkaTestEnvironment.brokersURL,
                    KEY_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java,
                    VALUE_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java,
                    SPECIFIC_AVRO_READER_CONFIG to true,
                    GROUP_ID_CONFIG to "LOL",
                    AUTO_OFFSET_RESET_CONFIG to "earliest",
                    ENABLE_AUTO_COMMIT_CONFIG to false,
                    "schema.registry.url" to kafkaTestEnvironment.schemaRegistry!!.url
            )
    )

    //Duration 4 seconds to allow for hendelse to be added to topic
    fun consumeHendelseTopic(): List<ConsumerRecord<HendelseKey, Hendelse>> {
        return hendelseTestConsumer.poll(Duration.ofSeconds(4)).records(KafkaConfig.PGI_HENDELSE_TOPIC).toList()
    }

    fun getFirstRecordOnTopic() = consumeHendelseTopic()[0]

    fun getLastRecordOnTopic(): ConsumerRecord<HendelseKey, Hendelse> {
        var hendelseRecordList: List<ConsumerRecord<HendelseKey, Hendelse>> = consumeHendelseTopic()
        var hendelsTempRecordList: List<ConsumerRecord<HendelseKey, Hendelse>>
        do {
            hendelsTempRecordList = consumeHendelseTopic()
            if (hendelsTempRecordList.isNotEmpty()) hendelseRecordList = hendelsTempRecordList
        } while (hendelsTempRecordList.isNotEmpty())

        return hendelseRecordList[hendelseRecordList.size - 1]
    }
}