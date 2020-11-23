package no.nav.pgi.skatt.leshendelse

import no.nav.pensjon.samhandling.naisserver.naisServer
import no.nav.pgi.skatt.leshendelse.kafka.KafkaConfig
import org.slf4j.LoggerFactory

private val LOGGER = LoggerFactory.getLogger(Application::class.java)

fun main() {
    val application = Application(KafkaConfig(), System.getenv())
    try {
        application.startHendelseSkattLoop()
    } catch (e: Throwable) {
        LOGGER.info(e.javaClass.name)
        application.stopServer()
    }
}

internal class Application(kafkaConfig: KafkaConfig, env: Map<String, String>, loopForever: Boolean = true) {
    private val naisServer = naisServer()
    private val hendelseSkattLoop = HendelseSkattLoop(kafkaConfig, env, loopForever)

    init {
        naisServer.start()
        addShutdownHook()
    }

    internal fun startHendelseSkattLoop() = hendelseSkattLoop.start()

    private fun addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread {
            stopServer()
        })
    }

    internal fun stopServer() {
        try {
            naisServer.stop(300, 300)
            LOGGER.info("naisServer stopped")
            hendelseSkattLoop.close()
            LOGGER.info("hendelseSkattLoop closed")
        } catch (e: Exception) {
            LOGGER.error("Error when when stopping naisServer and hendelseSkattLoop")
            LOGGER.error(e.message)
        }
    }
}

//TODO Tester for close
//TODO Kjør applikasjon mot mock
//TODO Legg inn logging
//TODO LEGG in matriser
//TODO DObbelsjekk tortuga-hiv om det er noe vi mangler