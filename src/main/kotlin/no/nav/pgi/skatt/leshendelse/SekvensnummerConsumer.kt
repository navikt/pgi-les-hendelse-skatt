package no.nav.pgi.skatt.leshendelse

import org.apache.kafka.common.TopicPartition
import java.time.Duration.ofSeconds

private const val POLLING_DURATION_SECONDS = 4L

internal class SekvensnummerConsumer(kafkaConfig: KafkaConfig, private val topicPartition: TopicPartition) {
    private val consumer = kafkaConfig.nextSekvensnummerConsumer()

    init {
        assignConsumerToPartition()
    }

    internal fun getLastSekvensnummer(): String? {
        pointToLastSekvensnummer()
        val sekvensnummerRecords = pollRecords()
        return if (sekvensnummerRecords.isEmpty()) null else sekvensnummerRecords.last().value()
    }

    internal fun close() = consumer.close()

    private fun assignConsumerToPartition() = consumer.assign(listOf(topicPartition))

    private fun pointToLastSekvensnummer() = consumer.seek(topicPartition, getOffsetOfLastRecord())

    private fun getOffsetOfLastRecord(): Long {
        val endOffset = endOffset()
        return if (endOffset > 0) endOffset - 1L else endOffset
    }

    private fun endOffset(): Long = consumer.endOffsets(mutableSetOf(topicPartition)).getLastRecordValue()

    private fun pollRecords() = consumer.poll(ofSeconds(POLLING_DURATION_SECONDS)).records(topicPartition).toList()
}


fun Map<TopicPartition, Long>.getLastRecordValue(): Long = entries.iterator().next().value

