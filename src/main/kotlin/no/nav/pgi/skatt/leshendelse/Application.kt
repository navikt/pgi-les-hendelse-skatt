package no.nav.pgi.skatt.leshendelse

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.pensjon.samhandling.liveness.isAlive
import no.nav.pensjon.samhandling.liveness.isReady
import no.nav.pensjon.samhandling.metrics.metrics
import no.nav.pgi.skatt.leshendelse.kafka.KafkaConfig


fun main() {
    createApplication().apply {
        start(wait = false)
    }
}

internal fun createApplication(serverPort: Int = 8080, kafkaConfig: KafkaConfig = KafkaConfig(), env: Map<String, String> = System.getenv(), loopForever: Boolean = true): NettyApplicationEngine {
    return embeddedServer(Netty, createApplicationEnvironment(serverPort, kafkaConfig, env, loopForever)).apply {
        addShutdownHook {
            //TODO Se om det finnes en finere måte å gjøre dette på
        }
    }

}


private fun createApplicationEnvironment(serverPort: Int, kafkaConfig: KafkaConfig, env: Map<String, String>, loopForever: Boolean) =
        applicationEngineEnvironment {
            connector { port = serverPort }
            module {
                isAlive()
                isReady()
                metrics()
                hendelseSkattLoop(kafkaConfig, env, loopForever)
            }
        }
