package no.nav.pgi.skatt.leshendelse

import no.nav.pgi.skatt.leshendelse.kafka.HendelseProducer
import no.nav.pgi.skatt.leshendelse.kafka.KafkaConfig
import no.nav.pgi.skatt.leshendelse.kafka.SekvensnummerConsumer
import no.nav.pgi.skatt.leshendelse.kafka.SekvensnummerProducer
import no.nav.pgi.skatt.leshendelse.skatt.*

internal const val ANTALL_HENDELSER = 1000

internal class HendelseSkattLoop(kafkaConfig: KafkaConfig, env: Map<String, String>, val loopForever: Boolean) {
    private val hendelseSkatt = HendelseSkatt(kafkaConfig, env)
    private val scheduler = SkattScheduler(env)
    private var stopLoop = false

    internal fun start() {
        do {
            hendelseSkatt.readAndWriteHendelserToTopic()
            scheduler.wait()
        } while (continueToLoop())
        hendelseSkatt.close()
    }

    private fun continueToLoop() = loopForever && !stopLoop

    internal fun stop() {
        stopLoop = true
    }
}

internal class HendelseSkatt(private val kafkaConfig: KafkaConfig, private val env: Map<String, String>) {
    private val hendelseProducer = HendelseProducer(kafkaConfig)
    private val nextSekvensnummerProducer = SekvensnummerProducer(kafkaConfig)
    private val hendelseClient = HendelseClient(env)

    internal fun readAndWriteHendelserToTopic() {
        var hendelserDto: HendelserDto
        var currentSekvensnummer = getInitialSekvensnummer()
        do {
            hendelserDto = hendelseClient.getHendelserSkatt(ANTALL_HENDELSER, currentSekvensnummer)
                    .apply {
                        hendelseProducer.writeHendelser(this)
                        if (getNextSekvensnummer() != USE_PREVIOUS_SEKVENSNUMMER) {
                            currentSekvensnummer = getNextSekvensnummer()
                            nextSekvensnummerProducer.writeSekvensnummer(getNextSekvensnummer())
                        }
                    }
        } while (hendelserDto.size() >= ANTALL_HENDELSER)
    }

    private fun getInitialSekvensnummer(): Long =
            SekvensnummerConsumer(kafkaConfig).getNextSekvensnummer()?.toLong()
                    ?: FirstSekvensnummerClient(env).getFirstSekvensnummerFromSkatt()

    fun close() {
        hendelseProducer.close()
        nextSekvensnummerProducer.close()
    }

}