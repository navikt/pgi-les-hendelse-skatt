package no.nav.pgi.skatt.leshendelse

import no.nav.pgi.skatt.leshendelse.kafka.KafkaFactory

internal const val ANTALL_HENDELSER = 1000

internal class HendelseSkattLoop(kafkaFactory: KafkaFactory, env: Map<String, String>, val loopForever: Boolean) {
    private val readAndWriteHendelserToTopicLoop = ReadAndWriteHendelserToTopicLoop(kafkaFactory, env)
    private val scheduler = SkattScheduler(env)

    internal fun start() {
        do {
            readAndWriteHendelserToTopicLoop.start()
            scheduler.wait()
        } while (loopForever)
    }

    internal fun close() {
        scheduler.close()
        readAndWriteHendelserToTopicLoop.close()
    }
}