package no.nav.pgi.skatt.leshendelse

import org.apache.kafka.common.TopicPartition
import java.time.Duration.ofSeconds

private const val POLLING_DURATION_SECONDS = 4L

internal class SekvensnummerConsumer(kafkaConfig: KafkaConfig, private val topicPartition: TopicPartition) {
    private val sekvensnummerConsumer = kafkaConfig.nextSekvensnummerConsumer()

    init {
        assignPartitionToConsumer()
    }

    internal fun getLastSekvensnummer(): String? {
        pointToLastSekvensnummer()
        return getSekvensnummerRecords().last().value()
    }

    private fun getSekvensnummerRecords() = sekvensnummerConsumer.poll(ofSeconds(POLLING_DURATION_SECONDS)).records(topicPartition).toList()

    private fun assignPartitionToConsumer() = sekvensnummerConsumer.assign(listOf(topicPartition))

    private fun pointToLastSekvensnummer() = sekvensnummerConsumer.seek(topicPartition, getLastSekvensnummerOffset())

    private fun getLastSekvensnummerOffset() = endOffsets().entries.iterator().next().value - 1L

    private fun endOffsets() = sekvensnummerConsumer.endOffsets(mutableSetOf(topicPartition))

}