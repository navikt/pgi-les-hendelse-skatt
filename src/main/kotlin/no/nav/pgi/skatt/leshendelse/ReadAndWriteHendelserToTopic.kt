package no.nav.pgi.skatt.leshendelse

import no.nav.pgi.skatt.leshendelse.kafka.HendelseProducer
import no.nav.pgi.skatt.leshendelse.kafka.KafkaConfig
import no.nav.pgi.skatt.leshendelse.kafka.SekvensnummerProducer
import no.nav.pgi.skatt.leshendelse.skatt.HendelseClient
import no.nav.pgi.skatt.leshendelse.skatt.HendelserDto
import no.nav.pgi.skatt.leshendelse.skatt.getNextSekvensnummer
import no.nav.pgi.skatt.leshendelse.skatt.size

internal const val ANTALL_HENDELSER = 1000

internal class HendelseSkattLoop(kafkaConfig: KafkaConfig, env: Map<String, String>, val loopForever: Boolean) {
    private val readAndWriteHendelserToTopic = ReadAndWriteHendelserToTopic(kafkaConfig, env)
    private val scheduler = SkattScheduler(env)

    internal fun start() {
        do {
            readAndWriteHendelserToTopic.start()
            scheduler.wait()
        } while (shouldContinueToLoop())
    }

    private fun shouldContinueToLoop() = loopForever

    internal fun close() {
        readAndWriteHendelserToTopic.close()
    }
}

internal class ReadAndWriteHendelserToTopic(private val kafkaConfig: KafkaConfig, private val env: Map<String, String>) {
    private val hendelseProducer = HendelseProducer(kafkaConfig)
    private val nextSekvensnummerProducer = SekvensnummerProducer(kafkaConfig)
    private val sekvensnummer = Sekvensnummer(kafkaConfig, env)
    private val hendelseClient = HendelseClient(env)
    private var stopped = false

    internal fun start() {
        var hendelserDto: HendelserDto
        do {
            if(stopped) return
            hendelserDto = hendelseClient.getHendelserSkatt(ANTALL_HENDELSER, sekvensnummer.value)
            hendelseProducer.writeHendelser(hendelserDto)
            sekvensnummer.value = hendelserDto.getNextSekvensnummer()
        } while (hendelserDto.size() >= ANTALL_HENDELSER && !stopped)
    }

    fun close() {
        hendelseProducer.close()
        nextSekvensnummerProducer.close()
        sekvensnummer.close()
    }

}