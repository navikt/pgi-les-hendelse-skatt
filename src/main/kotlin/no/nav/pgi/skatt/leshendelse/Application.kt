package no.nav.pgi.skatt.leshendelse

import no.nav.pensjon.samhandling.naisserver.naisServer
import no.nav.pgi.skatt.leshendelse.kafka.KafkaFactory
import no.nav.pgi.skatt.leshendelse.kafka.KafkaHendelseFactory
import org.slf4j.LoggerFactory

private val LOG = LoggerFactory.getLogger(Application::class.java)

fun main() {
    val application = Application(KafkaHendelseFactory(), System.getenv())
    try {
        application.startHendelseSkattLoop()
    } catch (e: Throwable) {
        LOG.info(e.message)
        application.stopServer()
    }
}

internal class Application(kafkaFactory: KafkaFactory, env: Map<String, String>, loopForever: Boolean = true) {
    private val naisServer = naisServer()
    private val hendelseSkattLoop = HendelseSkattLoop(kafkaFactory, env, loopForever)

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
            LOG.info("naisServer stopped")
            hendelseSkattLoop.close()
            LOG.info("hendelseSkattLoop closed")
        } catch (e: Exception) {
            LOG.error("Error when when stopping naisServer and hendelseSkattLoop")
            LOG.error(e.message)
        }
    }
}

//TODO Legg inn logging
//TODO LEGG in matriser
//TODO DObbelsjekk tortuga-hiv om det er noe vi mangler
