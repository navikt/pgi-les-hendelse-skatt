package no.nav.pgi.skatt.leshendelse

import io.ktor.application.*
import io.ktor.application.Application
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*

const val IS_ALIVE_PATH = "/isAlive"
const val IS_READY_PATH = "/isReady"

internal fun Application.liveness() {
    routing {
        probeRouting(IS_ALIVE_PATH)
        probeRouting(IS_READY_PATH)
    }
}

private fun Routing.probeRouting(path: String) {
    get(path) {
        call.respondText("", ContentType.Text.Plain, HttpStatusCode.OK)
    }
}