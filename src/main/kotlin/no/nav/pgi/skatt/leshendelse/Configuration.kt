package no.nav.pgi.skatt.leshendelse

import io.micrometer.core.instrument.MeterRegistry
import no.nav.pgi.skatt.leshendelse.kafka.KafkaFactoryImpl
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.EnableScheduling
import kotlin.system.exitProcess

@Configuration
@EnableScheduling
@Profile("dev-gcp", "prod-gcp")
class Configuration {

    @Bean
    fun applicationService(
        meterRegistry: MeterRegistry,
    ): ApplicationService {
        return ApplicationService(
            Counters(meterRegistry),
            KafkaFactoryImpl(),
            System.getenv()
        ) {
            log.info("Terminating ApplicationService")
            exitProcess(1)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(Configuration::class.java)
    }
}