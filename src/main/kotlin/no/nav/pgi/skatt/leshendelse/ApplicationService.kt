package no.nav.pgi.skatt.leshendelse

import no.nav.pgi.skatt.leshendelse.kafka.KafkaFactory
import no.nav.pgi.skatt.leshendelse.kafka.KafkaFactoryImpl
import no.nav.pgi.skatt.leshendelse.util.maskFnr
import org.slf4j.LoggerFactory

private val LOG = LoggerFactory.getLogger(ApplicationService::class.java)

fun serviceMain() {
    val applicationService = ApplicationService(KafkaFactoryImpl(), System.getenv())
    try {
        applicationService.startHendelseSkattLoop()
    } catch (e: Throwable) {
        val causeString = e.cause?.let { "Cause: ${it::class.simpleName}" }?:""
        LOG.warn("${e::class.simpleName} ${e.message?.maskFnr()} $causeString")
        applicationService.stopServer()
    }
}

internal class ApplicationService(kafkaFactory: KafkaFactory, env: Map<String, String>, loopForever: Boolean = true) {
    private val hendelseSkattLoop = HendelseSkattLoop(kafkaFactory, env, loopForever)

    init {
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
            LOG.info("naisServer stopped")
            hendelseSkattLoop.close()
            LOG.info("hendelseSkattLoop closed")
        } catch (e: Exception) {
            LOG.error("Error when when stopping naisServer and hendelseSkattLoop")
            LOG.error(e.message)
        }
    }
}

