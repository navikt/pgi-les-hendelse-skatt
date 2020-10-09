package no.nav.pgi.skatt.leshendelse

import io.ktor.application.*
import no.nav.pgi.skatt.leshendelse.skatt.*
import no.nav.pgi.skatt.leshendelse.skatt.FirstSekvensnummerClient
import no.nav.pgi.skatt.leshendelse.skatt.HendelseClient

private const val ANTALL_HENDELSER = 1000

internal fun Application.hendelseSkatt(kafkaConfig: KafkaConfig, env: Map<String, String>) {
    val hendelseProducer = HendelseProducer(kafkaConfig)
    val nextSekvensnummerProducer = kafkaConfig.nextSekvensnummerProducer()
    val hendelseClient = HendelseClient(env)

    var sekvensnummer = hentSekvensnummer(kafkaConfig, env)


    var hendelserDto: HendelserDto

    do{
        hendelserDto = hendelseClient.getHendelserSkatt(ANTALL_HENDELSER, sekvensnummer)
        hendelserDto.hendelser.forEach {hendelseProducer.writeHendelse(it)}
        sekvensnummer = hendelserDto.nestesekvensnr

    }while(hendelserDto.size() >= ANTALL_HENDELSER)






    //TODO Hent neste sekvensnummer
    //Forsøk å hent lagret sekvensnummer
    //TODO Hent første sekvensnummer
    //TODO Les hendelser med sekvensnummer og antall
    //TODO Skriv hendelser til kafka kø
    //TODO Oppdater neste sekvensnummer


}


private fun hentSekvensnummer(kafkaConfig: KafkaConfig, env: Map<String, String>): Long =
        SekvensnummerConsumer(kafkaConfig).getNextSekvensnummer()?.toLong()
                ?: FirstSekvensnummerClient(env).getFirstSekvensnummerFromSkatt()



