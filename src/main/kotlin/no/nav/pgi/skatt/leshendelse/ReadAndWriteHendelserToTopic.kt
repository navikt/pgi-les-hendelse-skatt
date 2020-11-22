package no.nav.pgi.skatt.leshendelse

import no.nav.pgi.skatt.leshendelse.kafka.HendelseProducer
import no.nav.pgi.skatt.leshendelse.kafka.KafkaConfig
import no.nav.pgi.skatt.leshendelse.kafka.SekvensnummerProducer
import no.nav.pgi.skatt.leshendelse.skatt.HendelseClient
import no.nav.pgi.skatt.leshendelse.skatt.HendelserDto
import no.nav.pgi.skatt.leshendelse.skatt.getNextSekvensnummer
import no.nav.pgi.skatt.leshendelse.skatt.size

internal const val ANTALL_HENDELSER = 1000

internal class HendelseSkattLoop(kafkaConfig: KafkaConfig, env: Map<String, String>, val loopForever: Boolean) : Stop() {
    private val readAndWriteHendelserToTopic = ReadAndWriteHendelserToTopic(kafkaConfig, env)
    private val scheduler = SkattScheduler(env)

    internal fun start() {
        do {
            readAndWriteHendelserToTopic.start()
            scheduler.wait()
        } while (shouldContinueToLoop() && isNotStopped())
        close()
    }

    private fun shouldContinueToLoop() = loopForever

    override fun onStop(){
        readAndWriteHendelserToTopic.stop()
        scheduler.stop()
    }

    internal fun close() {
        readAndWriteHendelserToTopic.close()
    }
}

internal class ReadAndWriteHendelserToTopic(kafkaConfig: KafkaConfig, env: Map<String, String>) : Stop() {
    private val hendelseProducer = HendelseProducer(kafkaConfig)
    private val nextSekvensnummerProducer = SekvensnummerProducer(kafkaConfig)
    private val sekvensnummer = Sekvensnummer(kafkaConfig, env)
    private val hendelseClient = HendelseClient(env)

    internal fun start() {
        var hendelserDto: HendelserDto
        do {
            if(isStopped()) break
            hendelserDto = hendelseClient.getHendelserSkatt(ANTALL_HENDELSER, sekvensnummer.value)
            hendelseProducer.writeHendelser(hendelserDto)
            sekvensnummer.value = hendelserDto.getNextSekvensnummer()
        } while (hendelserDto.size() >= ANTALL_HENDELSER && isNotStopped())
    }

    fun close() {
        hendelseProducer.close()
        nextSekvensnummerProducer.close()
        sekvensnummer.close()
    }

}