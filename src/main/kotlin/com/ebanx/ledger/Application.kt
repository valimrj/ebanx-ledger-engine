package com.ebanx.ledger

import com.ebanx.ledger.http.businessRouting
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json

/**
 * Process entry point. I only start the Ktor server on the configured port
 * here; no business rule lives in this class.
 */
fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

/**
 * Application composition: transport plugins (JSON) and the business
 * routes, which live in `http.businessRouting` - it also installs the
 * `StatusPages` plugin that maps domain exceptions to HTTP status codes.
 *
 * `explicitNulls = false` makes the serializer omit null fields, because the
 * assignment only includes the "origin" or "destination" key when it
 * applies to the event type.
 */
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
fun Application.module() {
    install(ContentNegotiation) {
        json(Json { explicitNulls = false })
    }
    businessRouting()
}
