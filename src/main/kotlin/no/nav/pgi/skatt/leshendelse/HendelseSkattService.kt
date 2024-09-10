package no.nav.pgi.skatt.leshendelse

import no.nav.pgi.skatt.leshendelse.kafka.KafkaFactory

internal const val ANTALL_HENDELSER = 1000

internal class HendelseSkattService(
    private val counters: Counters,
    kafkaFactory: KafkaFactory,
    env: Map<String, String>,
) {
    private val readAndWriteHendelserToTopicLoop = ReadAndWriteHendelserToTopicLoop(
        counters = counters,
        kafkaFactory = kafkaFactory,
        env = env
    )
    private val skattTimer = SkattTimer(env)

    internal fun readAndWriteAvailableHendelserToTopicAndDelay() {
        readAndWriteHendelserToTopicLoop.processHendelserFromSkattWhileAboveThreshold()
        skattTimer.delay()
    }

    internal fun close() {
        skattTimer.close()
        readAndWriteHendelserToTopicLoop.close()
    }
}