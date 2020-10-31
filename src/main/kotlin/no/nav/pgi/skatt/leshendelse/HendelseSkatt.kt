package no.nav.pgi.skatt.leshendelse

import no.nav.pgi.skatt.leshendelse.kafka.HendelseProducer
import no.nav.pgi.skatt.leshendelse.kafka.KafkaConfig
import no.nav.pgi.skatt.leshendelse.kafka.SekvensnummerConsumer
import no.nav.pgi.skatt.leshendelse.kafka.SekvensnummerProducer
import no.nav.pgi.skatt.leshendelse.skatt.*

internal const val ANTALL_HENDELSER = 1000

internal fun hendelseSkattLoop(kafkaConfig: KafkaConfig, env: Map<String, String>, loopForever: Boolean) {
    val hendelseSkatt = HendelseSkatt(kafkaConfig, env)
    val scheduler = SkattScheduler(env)
    do {
        hendelseSkatt.readAndWriteHendelserToTopic()
        scheduler.wait()
    } while (loopForever)
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
                        if (getNesteSekvensnummer() != USE_PREVIOUS_SEKVENSNUMMER) {
                            currentSekvensnummer = getNesteSekvensnummer()
                            nextSekvensnummerProducer.writeSekvensnummer(getNesteSekvensnummer())
                        }
                    }
        } while (hendelserDto.size() >= ANTALL_HENDELSER)
    }

    private fun getInitialSekvensnummer(): Long =
            SekvensnummerConsumer(kafkaConfig).getNextSekvensnummer()?.toLong()
                    ?: FirstSekvensnummerClient(env).getFirstSekvensnummerFromSkatt()

}