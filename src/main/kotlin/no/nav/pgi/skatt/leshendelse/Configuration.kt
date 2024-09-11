package no.nav.pgi.skatt.leshendelse

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.pgi.skatt.leshendelse.kafka.KafkaFactoryImpl
import no.nav.pgi.skatt.leshendelse.util.maskFnr
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.EnableScheduling

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
        )
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(Configuration::class.java)
    }
}