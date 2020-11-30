package no.nav.pgi.skatt.leshendelse

import no.nav.pgi.skatt.leshendelse.kafka.KafkaFactory

internal const val ANTALL_HENDELSER = 1000

internal class HendelseSkattLoop(kafkaFactory: KafkaFactory, env: Map<String, String>, val loopForever: Boolean) {
    private val readAndWriteHendelserToTopicLoop = ReadAndWriteHendelserToTopicLoop(kafkaFactory, env)
    private val skattTimer = SkattTimer(env)

    internal fun start() {
        do {
            readAndWriteHendelserToTopicLoop.start()
            skattTimer.delay()
        } while (loopForever)
    }

    internal fun close() {
        skattTimer.close()
        readAndWriteHendelserToTopicLoop.close()
    }
}