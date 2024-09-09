package no.nav.pgi.skatt.leshendelse.common

import no.nav.common.KafkaEnvironment
import no.nav.common.KafkaEnvironment.TopicInfo
import no.nav.pgi.skatt.leshendelse.kafka.KafkaConfig
import no.nav.pgi.skatt.leshendelse.kafka.NEXT_SEKVENSNUMMER_TOPIC
import no.nav.pgi.skatt.leshendelse.kafka.PGI_HENDELSE_TOPIC
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig.*
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration

class KafkaTestEnvironment {

    private val kafkaTestEnvironment: KafkaEnvironment = KafkaEnvironment(
        withSchemaRegistry = true,
        topicInfos = listOf(
            TopicInfo(NEXT_SEKVENSNUMMER_TOPIC, partitions = 1),
            TopicInfo(PGI_HENDELSE_TOPIC, partitions = 1)
        )
    )

    private var hendelseTestConsumer: KafkaConsumer<String, String> = hendelseTestConsumer()

    init {
        kafkaTestEnvironment.start()
        hendelseTestConsumer.subscribe(listOf(PGI_HENDELSE_TOPIC))
    }

    internal fun tearDown() = kafkaTestEnvironment.tearDown()

    internal fun kafkaTestEnvironmentVariables() = mapOf(
        KafkaConfig.BOOTSTRAP_SERVERS to kafkaTestEnvironment.brokersURL,
    )

    private fun hendelseTestConsumer() = KafkaConsumer<String, String>(
        mapOf(
            CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to kafkaTestEnvironment.brokersURL,
            KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            GROUP_ID_CONFIG to "LOL",
            AUTO_OFFSET_RESET_CONFIG to "earliest",
            ENABLE_AUTO_COMMIT_CONFIG to false,
            "schema.registry.url" to kafkaTestEnvironment.schemaRegistry!!.url
        )
    )

    //Duration 4 seconds to allow for hendelse to be added to topic
    fun consumeHendelseTopic(): List<ConsumerRecord<String, String>> =
        hendelseTestConsumer.poll(Duration.ofSeconds(4)).records(PGI_HENDELSE_TOPIC).toList()

    fun getFirstRecordOnTopic() = consumeHendelseTopic()[0]

    fun getLastRecordOnTopic(): ConsumerRecord<String, String> {
        var hendelseRecordList: List<ConsumerRecord<String, String>> = consumeHendelseTopic()
        var hendelsTempRecordList: List<ConsumerRecord<String, String>>
        do {
            hendelsTempRecordList = consumeHendelseTopic()
            if (hendelsTempRecordList.isNotEmpty()) hendelseRecordList = hendelsTempRecordList
        } while (hendelsTempRecordList.isNotEmpty())

        return hendelseRecordList[hendelseRecordList.size - 1]
    }
}