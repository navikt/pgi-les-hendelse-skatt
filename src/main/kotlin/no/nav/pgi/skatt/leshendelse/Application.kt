package no.nav.pgi.skatt.leshendelse

import io.ktor.server.engine.*
import io.ktor.server.netty.*


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
