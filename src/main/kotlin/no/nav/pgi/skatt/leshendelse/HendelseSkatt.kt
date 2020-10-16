package no.nav.pgi.skatt.leshendelse

import io.ktor.application.*
import no.nav.pgi.skatt.leshendelse.kafka.HendelseProducer
import no.nav.pgi.skatt.leshendelse.kafka.KafkaConfig
import no.nav.pgi.skatt.leshendelse.kafka.SekvensnummerConsumer
import no.nav.pgi.skatt.leshendelse.kafka.SekvensnummerProducer
import no.nav.pgi.skatt.leshendelse.skatt.FirstSekvensnummerClient
import no.nav.pgi.skatt.leshendelse.skatt.HendelseClient
import no.nav.pgi.skatt.leshendelse.skatt.HendelserDto

private const val ANTALL_HENDELSER = 1000

internal fun Application.hendelseSkattLoop(kafkaConfig: KafkaConfig, env: Map<String, String>, loopForever: Boolean) {
    val hendelseSkatt = HendelseSkatt(kafkaConfig, env)
    val scheduler: SkattScheduler = SkattScheduler(env)
    do {
        hendelseSkatt.readAndWriteHendelserToTopic()
        //scheduler.process()
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
                        currentSekvensnummer = nestesekvensnr
                        nextSekvensnummerProducer.writeSekvensnummer(nestesekvensnr)
                    }
        } while (hendelserDto.size() >= ANTALL_HENDELSER)
    }

    private fun getInitialSekvensnummer(): Long =
            SekvensnummerConsumer(kafkaConfig).getNextSekvensnummer()?.toLong()
                    ?: FirstSekvensnummerClient(env).getFirstSekvensnummerFromSkatt()

}