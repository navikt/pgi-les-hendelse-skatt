package no.nav.pgi.skatt.leshendelse

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.pgi.skatt.leshendelse.kafka.KafkaFactory
import no.nav.pgi.skatt.leshendelse.kafka.KafkaFactoryImpl
import no.nav.pgi.skatt.leshendelse.util.maskFnr
import org.slf4j.LoggerFactory

fun serviceMain() {
    val LOG = LoggerFactory.getLogger(ApplicationService::class.java)

    val applicationService = ApplicationService(
        Counters(SimpleMeterRegistry()), // TODO: midlertidig, frem til spring-wiring er på plass
        KafkaFactoryImpl(),
        System.getenv()
    )
    try {
        do {
            applicationService.lesOgSkrivHendelser()
        } while (true)
    } catch (e: Throwable) {
        val causeString = e.cause?.let { "Cause: ${it::class.simpleName}" }?:""
        LOG.warn("${e::class.simpleName} ${e.message?.maskFnr()} $causeString")
        applicationService.stopHendelseSkattService()
    }
}


class ApplicationService(
    private val counters: Counters,
    kafkaFactory: KafkaFactory,
    env: Map<String, String>,
) {
    private val hendelseSkattService = HendelseSkattService(
        counters = counters,
        kafkaFactory = kafkaFactory,
        env = env,
    )

    init {
        addShutdownHook()
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
            LOG.info("hendelseSkattLoop closed")
        } catch (e: Exception) {
            LOG.error("Error when when stopping hendelseSkattService")
            LOG.error(e.message)
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ApplicationService::class.java)
    }
}

