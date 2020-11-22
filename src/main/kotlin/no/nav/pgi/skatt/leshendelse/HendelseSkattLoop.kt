package no.nav.pgi.skatt.leshendelse

import no.nav.pgi.skatt.leshendelse.kafka.KafkaConfig

internal const val ANTALL_HENDELSER = 1000

internal class HendelseSkattLoop(kafkaConfig: KafkaConfig, env: Map<String, String>, val loopForever: Boolean) : Stop() {
    private val readAndWriteHendelserToTopic = ReadAndWriteHendelserToTopicLoop(kafkaConfig, env)
    private val scheduler = SkattScheduler(env)

    internal fun start() {
        do {
            readAndWriteHendelserToTopic.start()
            scheduler.wait()
        } while (shouldContinueToLoop() && isNotStopped())
        close()
    }

    private fun shouldContinueToLoop() = loopForever

    override fun onStop() {
        readAndWriteHendelserToTopic.stop()
        scheduler.stop()
    }

    internal fun close() {
        readAndWriteHendelserToTopic.close()
    }
}