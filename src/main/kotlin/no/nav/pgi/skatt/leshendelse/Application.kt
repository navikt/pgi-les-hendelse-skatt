package no.nav.pgi.skatt.leshendelse

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.pensjon.samhandling.liveness.isAlive
import no.nav.pensjon.samhandling.liveness.isReady
import no.nav.pensjon.samhandling.metrics.metrics


fun main() {
    createApplication().apply {
        start(wait = false)
    }
}

internal fun createApplication(serverPort: Int = 8080, kafkaConfig: KafkaConfig = KafkaConfig(), env: Map<String, String> = System.getenv()) =
        embeddedServer(Netty, createApplicationEnvironment(serverPort, kafkaConfig, env))

private fun createApplicationEnvironment(serverPort: Int, kafkaConfig: KafkaConfig, env: Map<String, String>) =
        applicationEngineEnvironment {
            connector { port = serverPort }
            module {
                isAlive()
                isReady()
                metrics()
                hendelseSkatt(kafkaConfig, env)
            }
        }
