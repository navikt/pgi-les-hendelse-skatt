package no.nav.pgi.skatt.leshendelse

import io.prometheus.client.Counter
import no.nav.pensjon.samhandling.naisserver.naisServer
import no.nav.pgi.skatt.leshendelse.kafka.KafkaConfig
import org.slf4j.LoggerFactory

private val LOG = LoggerFactory.getLogger(Application::class.java)

fun main() {
    val application = Application(KafkaConfig(), System.getenv())
    try {
        application.startHendelseSkattLoop()
    } catch (e: Throwable) {
        LOG.info(e.message)
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
            LOG.info("naisServer stopped")
            hendelseSkattLoop.close()
            LOG.info("hendelseSkattLoop closed")
        } catch (e: Exception) {
            LOG.error("Error when when stopping naisServer and hendelseSkattLoop")
            LOG.error(e.message)
        }
    }
}


private val antallHendelserSendt = Counter.build()

        .name("hendelser_processed")
        .help("Antall hendelser sendt.").register()


private val apepikk = Counter.build()

//TODO Legg inn logging
//TODO Kjør applikasjon mot mock
//TODO LEGG in matriser
//TODO asynkrone producer kall for hendelse
//TODO DObbelsjekk tortuga-hiv om det er noe vi mangler