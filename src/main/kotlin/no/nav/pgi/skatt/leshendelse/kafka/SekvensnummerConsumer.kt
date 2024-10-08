package no.nav.pgi.skatt.leshendelse.kafka

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory
import java.time.Duration.ofSeconds
import kotlin.math.max

private val LOG = LoggerFactory.getLogger(SekvensnummerConsumer::class.java)

internal class SekvensnummerConsumer(
    private val consumer: Consumer<String, String>,
    private val topicPartition: TopicPartition = defaultTopicPartition,
) {
    init {
        consumer.assign(listOf(topicPartition))
    }

    internal fun getNextSekvensnummer(): String? {
        setPollOffset(lastSekvensnummerOffset())
        return pollRecords().lastValue()
            .also { LOG.info("""Polled sekvensnummer $it from topic ${topicPartition.topic()}""") }
    }

    private fun lastSekvensnummerOffset(): Long = max(getEndOffset() - 1, 0)

    private fun getEndOffset(): Long = consumer.endOffsets(mutableSetOf(topicPartition))[topicPartition]!!

    private fun setPollOffset(offset: Long) = consumer.seek(topicPartition, offset)

    private fun pollRecords() = consumer.poll(ofSeconds(POLLING_DURATION_SECONDS)).records(topicPartition).toList()

    internal fun close() = consumer.close().also { LOG.info("nextSekvensnummerConsumer closed") }

    companion object {
        private const val POLLING_DURATION_SECONDS = 4L
        internal val defaultTopicPartition = TopicPartition(NEXT_SEKVENSNUMMER_TOPIC, 0)
    }
}

private fun List<ConsumerRecord<String, String>>.lastValue() = if (isEmpty()) null else last().value()
