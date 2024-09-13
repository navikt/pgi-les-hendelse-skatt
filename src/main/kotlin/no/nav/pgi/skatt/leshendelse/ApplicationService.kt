package no.nav.pgi.skatt.leshendelse

import no.nav.pgi.skatt.leshendelse.kafka.KafkaFactory
import no.nav.pgi.skatt.leshendelse.util.maskFnr
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled

class ApplicationService(
    private val counters: Counters,
    kafkaFactory: KafkaFactory,
    env: Map<String, String>,
    val stopApplication: () -> Unit,
) {
    private val hendelseSkattService = HendelseSkattService(
        counters = counters,
        kafkaFactory = kafkaFactory,
        env = env,
    )

    init {
        addShutdownHook()
    }

    @Scheduled(fixedDelay = 3_000) // sover 3 sekunder mellom hver iterasjon
    fun runIteration() {
        try {
            lesOgSkrivHendelser()
        } catch (e: Throwable) {
            stopApplicationFromException(e)
        }
    }

    internal fun lesOgSkrivHendelser() {
        hendelseSkattService.readAndWriteAvailableHendelserToTopicAndDelay()
    }

    private fun addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread {
            stopHendelseSkattService()
        })
    }

    internal fun stopHendelseSkattService() {
        try {
            hendelseSkattService.close()
            log.info("hendelseSkattLoop closed")
        } catch (e: Exception) {
            log.error("Error when when stopping hendelseSkattService")
            log.error(e.message)
        }
    }

    private fun stopApplicationFromException(e: Throwable) {
        val causeString = e.cause?.let { "Cause: ${it::class.simpleName}" } ?: ""
        log.warn("${e::class.simpleName} ${e.message?.maskFnr()} $causeString")
        stopHendelseSkattService()
        stopApplication()
    }

    companion object {
        private val log = LoggerFactory.getLogger(ApplicationService::class.java)
    }
}

