package no.nav.pgi.skatt.leshendelse

import no.nav.pensjon.samhandling.naisserver.naisServer
import no.nav.pgi.skatt.leshendelse.kafka.KafkaConfig
import org.slf4j.LoggerFactory

private val LOGGER = LoggerFactory.getLogger(Application::class.java)

fun main() {
    val application = Application(KafkaConfig(), System.getenv())
    try {
        // application.startHendelseSkattLoop()
    } catch (e: Throwable) {
        LOGGER.info(e.javaClass.name)
        // application.stopServer() TODO vent til kafka-gjengen har fiksa greiene sine
    }
}

internal class Application(kafkaConfig: KafkaConfig, env: Map<String, String>, loopForever: Boolean = true) {
    private val naisServer = naisServer()
    private val hendelseSkattLoop = HendelseSkattLoop(kafkaConfig, env, loopForever)

    init {
        naisServer.start()
    }

    internal fun startHendelseSkattLoop() = hendelseSkattLoop.start()

    internal fun stopServer() {
        try{
            hendelseSkattLoop.stop()
            naisServer.stop(100, 100)
        }catch (e:Exception){
            hendelseSkattLoop.close()
        }
    }
}


//TODO Vurder skriving sekvensnummer-topic bør være async. Fart!!
//TODO Tester for close og stop
//TODO shutdown hook.
//TODO Legg inn igjen hendelse HendelseSkattLoop for test
//TODO Kjør applikasjon mot mock