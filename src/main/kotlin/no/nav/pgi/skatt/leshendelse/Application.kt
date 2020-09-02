package no.nav.pgi.skatt.leshendelse

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.pgi.skatt.leshendelse.Kafka.hendelseSkatt


fun main() {
    createApplication().apply {
        start(wait = false)
    }
}

internal fun createApplication(serverPort: Int = 8080) =
        embeddedServer(Netty, createApplicationEnvironment(serverPort))

private fun createApplicationEnvironment(serverPort: Int) =
        applicationEngineEnvironment {
            connector { port = serverPort }
            module {
                liveness()
                hendelseSkatt()
            }
        }
