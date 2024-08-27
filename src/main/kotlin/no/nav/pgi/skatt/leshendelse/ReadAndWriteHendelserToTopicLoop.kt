package no.nav.pgi.skatt.leshendelse

import no.nav.pgi.skatt.leshendelse.kafka.FailedHendelse
import no.nav.pgi.skatt.leshendelse.kafka.HendelseProducer
import no.nav.pgi.skatt.leshendelse.kafka.KafkaFactory
import no.nav.pgi.skatt.leshendelse.skatt.HendelseClient
import no.nav.pgi.skatt.leshendelse.skatt.HendelseDto
import no.nav.pgi.skatt.leshendelse.skatt.getNextSekvensnummer
import org.slf4j.LoggerFactory

internal class ReadAndWriteHendelserToTopicLoop(kafkaFactory: KafkaFactory, env: Map<String, String>) {
    private val hendelseProducer = HendelseProducer(kafkaFactory)
    private val sekvensnummer = Sekvensnummer(kafkaFactory, env)
    private val hendelseClient = HendelseClient(env)

    internal fun start() {
        var hendelser: List<HendelseDto>
        do {
            hendelser = hendelseClient.getHendelserSkatt(ANTALL_HENDELSER, sekvensnummer.getSekvensnummer())
            hendelseProducer.writeHendelser(hendelser)?.let { handleFailedHendelse(it) }
            sekvensnummer.setSekvensnummer(hendelser.getNextSekvensnummer())
        } while (hendelser.size >= ANTALL_HENDELSER)
    }

    private fun handleFailedHendelse(failedHendelse: FailedHendelse) {
        sekvensnummer.addSekvensnummerToTopic(failedHendelse.hendelse.sekvensnummer, synchronous = true)
        throw failedHendelse.exception
    }

    internal fun close() {
        hendelseProducer.close()
        sekvensnummer.close()
    }
}