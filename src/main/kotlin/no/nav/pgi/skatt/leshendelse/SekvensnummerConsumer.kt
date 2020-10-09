package no.nav.pgi.skatt.leshendelse

import org.apache.kafka.common.TopicPartition
import java.time.Duration.ofSeconds

private const val POLLING_DURATION_SECONDS = 4L
private val defaultTopicPartition = TopicPartition(KafkaConfig.NEXT_SEKVENSNUMMER_TOPIC, 0)

internal class SekvensnummerConsumer(kafkaConfig: KafkaConfig, private val topicPartition: TopicPartition = defaultTopicPartition) {
    private val consumer = kafkaConfig.nextSekvensnummerConsumer()

    init {
        assignConsumerToPartition()
    }

    internal fun getNextSekvensnummer(): String? {
        pointToLastSekvensnummer()
        val sekvensnummerRecords = pollRecords()
        val lastSekvensnummer = if (sekvensnummerRecords.isEmpty()) null else sekvensnummerRecords.last().value()
        return lastSekvensnummer
    }

    private fun pointToLastSekvensnummer() = consumer.seek(topicPartition, getOffsetOfLastRecord())

    private fun getOffsetOfLastRecord(): Long {
        val endOffset = endOffset()
        return if (endOffset > 0) endOffset - 1L else endOffset
    }

    private fun endOffset(): Long = consumer.endOffsets(mutableSetOf(topicPartition)).getLastRecordValue()

    private fun pollRecords() = consumer.poll(ofSeconds(POLLING_DURATION_SECONDS)).records(topicPartition).toList()

    private fun assignConsumerToPartition() = consumer.assign(listOf(topicPartition))

    internal fun close() = consumer.close()
}


fun Map<TopicPartition, Long>.getLastRecordValue(): Long = entries.iterator().next().value

