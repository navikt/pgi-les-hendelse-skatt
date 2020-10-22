package no.nav.pgi.skatt.leshendelse

import no.nav.pensjon.samhandling.naisserver.naisServer
import no.nav.pgi.skatt.leshendelse.kafka.KafkaConfig
import no.nav.pgi.skatt.leshendelse.skatt.FirstSekvensnummerClient
import org.slf4j.LoggerFactory

private val LOGGER = LoggerFactory.getLogger(Application::class.java)

fun main() {
    val application = Application()
    try {
        application.startHendelseSkattLoop(KafkaConfig(), System.getenv())
    } catch (e: Throwable) {
        LOGGER.info(e.javaClass.name)
        //application.stopServer() TODO vent til kafka-gjengen har fiksa greiene sine
    }
}

internal class Application {
    private val naisServer = naisServer()

    init {
        naisServer.start()
    }

    internal fun startHendelseSkattLoop(kafkaConfig: KafkaConfig, env: Map<String, String>, loopForever: Boolean = true) =
            hendelseSkattLoop(kafkaConfig, env, loopForever)


    internal fun stopServer() {
        naisServer.stop(100, 100)
    }
}
