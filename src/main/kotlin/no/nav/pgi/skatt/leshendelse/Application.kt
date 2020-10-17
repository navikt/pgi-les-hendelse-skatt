package no.nav.pgi.skatt.leshendelse

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.pensjon.samhandling.liveness.isAlive
import no.nav.pensjon.samhandling.liveness.isReady
import no.nav.pensjon.samhandling.metrics.metrics
import no.nav.pgi.skatt.leshendelse.kafka.KafkaConfig


fun main() {
    val application = Application()
    application.startHendelseSkattLoop(KafkaConfig(), System.getenv())
}

internal class Application {

    private val naisServer = embeddedServer(Netty, createApplicationEnvironment())

    init {
        naisServer.addShutdownHook { /*TODO Se om  det finnes en finere måte å gjøre dette på.*/ }
        naisServer.start()
    }

    internal fun startHendelseSkattLoop(kafkaConfig: KafkaConfig, env: Map<String, String>, loopForever: Boolean = true) =
            hendelseSkattLoop(kafkaConfig, env, loopForever)

    private fun createApplicationEnvironment(serverPort: Int = 8080) =
            applicationEngineEnvironment {
                connector { port = serverPort }
                module {
                    isAlive()
                    isReady()
                    metrics()
                }
            }

    internal fun stopServer() {
        naisServer.stop(100, 100)
    }
}
