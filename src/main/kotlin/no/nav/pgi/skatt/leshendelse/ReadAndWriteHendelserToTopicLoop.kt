package no.nav.pgi.skatt.leshendelse

import no.nav.pgi.skatt.leshendelse.kafka.FailedHendelse
import no.nav.pgi.skatt.leshendelse.kafka.HendelseProducer
import no.nav.pgi.skatt.leshendelse.kafka.KafkaFactory
import no.nav.pgi.skatt.leshendelse.skatt.HendelseClient
import no.nav.pgi.skatt.leshendelse.skatt.HendelserDto
import no.nav.pgi.skatt.leshendelse.skatt.getNextSekvensnummer
import no.nav.pgi.skatt.leshendelse.skatt.size
import org.slf4j.LoggerFactory

private val LOG = LoggerFactory.getLogger(ReadAndWriteHendelserToTopicLoop::class.java.simpleName)

internal class ReadAndWriteHendelserToTopicLoop(kafkaFactory: KafkaFactory, env: Map<String, String>) {
    private val hendelseProducer = HendelseProducer(kafkaFactory)
    private val sekvensnummer = Sekvensnummer(kafkaFactory, env)
    private val hendelseClient = HendelseClient(env)

    internal fun start() {
        LOG.info("Starting to read pgi-hendelser and writing them to topic")
        var hendelserDto: HendelserDto
        do {
            hendelserDto = hendelseClient.getHendelserSkatt(ANTALL_HENDELSER, sekvensnummer.value)
            hendelseProducer.writeHendelser(hendelserDto)?.let { handleFailedHendelse(it) }
            sekvensnummer.value = hendelserDto.getNextSekvensnummer()
        } while (hendelserDto.size() >= ANTALL_HENDELSER)
        LOG.info("Stopped reading hendelser from skatt because antall hendelser was less then $ANTALL_HENDELSER")
    }

    private fun handleFailedHendelse(failedHendelse: FailedHendelse) {
        sekvensnummer.addSekvensnummerToTopic(failedHendelse.hendelse.getSekvensnummer(), synchronous = true)
        throw failedHendelse.exception
    }

    internal fun close() {
        hendelseProducer.close()
        sekvensnummer.close()
    }
}