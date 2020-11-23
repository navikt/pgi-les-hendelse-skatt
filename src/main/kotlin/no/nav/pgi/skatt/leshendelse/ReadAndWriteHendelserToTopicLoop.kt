package no.nav.pgi.skatt.leshendelse

import no.nav.pgi.skatt.leshendelse.kafka.HendelseProducer
import no.nav.pgi.skatt.leshendelse.kafka.KafkaConfig
import no.nav.pgi.skatt.leshendelse.skatt.HendelseClient
import no.nav.pgi.skatt.leshendelse.skatt.HendelserDto
import no.nav.pgi.skatt.leshendelse.skatt.getNextSekvensnummer
import no.nav.pgi.skatt.leshendelse.skatt.size
import org.slf4j.LoggerFactory

private val LOG = LoggerFactory.getLogger(ReadAndWriteHendelserToTopicLoop::class.java)

internal class ReadAndWriteHendelserToTopicLoop(kafkaConfig: KafkaConfig, env: Map<String, String>) {
    private val hendelseProducer = HendelseProducer(kafkaConfig)
    private val sekvensnummer = Sekvensnummer(kafkaConfig, env)
    private val hendelseClient = HendelseClient(env)

    internal fun start() {
        LOG.info("starting to read hendelser from skatt")
        var hendelserDto: HendelserDto
        do {
            hendelserDto = hendelseClient.getHendelserSkatt(ANTALL_HENDELSER, sekvensnummer.value)
            hendelseProducer.writeHendelser(hendelserDto)
            sekvensnummer.value = hendelserDto.getNextSekvensnummer()
        } while (hendelserDto.size() >= ANTALL_HENDELSER)
        LOG.info("Stop reading hendelser from skatt because antall hendelser was less then $ANTALL_HENDELSER")
    }

    internal fun close() {
        hendelseProducer.close()
        sekvensnummer.close()
    }
}