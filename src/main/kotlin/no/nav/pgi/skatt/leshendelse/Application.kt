package no.nav.pgi.skatt.leshendelse

import no.nav.pensjon.samhandling.naisserver.naisServer
import no.nav.pgi.skatt.leshendelse.kafka.KafkaConfig


fun main() {
    val application = Application()
    try {
        //application.startHendelseSkattLoop(KafkaConfig(), System.getenv())
    } catch (e: Exception) {
        application.stopServer()
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
