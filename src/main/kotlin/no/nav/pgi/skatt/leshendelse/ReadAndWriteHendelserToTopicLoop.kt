package no.nav.pgi.skatt.leshendelse

import no.nav.pgi.skatt.leshendelse.kafka.HendelseProducer
import no.nav.pgi.skatt.leshendelse.kafka.KafkaConfig
import no.nav.pgi.skatt.leshendelse.skatt.HendelseClient
import no.nav.pgi.skatt.leshendelse.skatt.HendelserDto
import no.nav.pgi.skatt.leshendelse.skatt.getNextSekvensnummer
import no.nav.pgi.skatt.leshendelse.skatt.size

internal class ReadAndWriteHendelserToTopicLoop(kafkaConfig: KafkaConfig, env: Map<String, String>) {
    private val hendelseProducer = HendelseProducer(kafkaConfig)
    private val sekvensnummer = Sekvensnummer(kafkaConfig, env)
    private val hendelseClient = HendelseClient(env)

    internal fun start() {
        var hendelserDto: HendelserDto
        do {
            hendelserDto = hendelseClient.getHendelserSkatt(ANTALL_HENDELSER, sekvensnummer.value)
            hendelseProducer.writeHendelser(hendelserDto)
            sekvensnummer.value = hendelserDto.getNextSekvensnummer()
        } while (hendelserDto.size() >= ANTALL_HENDELSER)
    }

    internal fun close() {
        hendelseProducer.close()
        sekvensnummer.close()
    }
}