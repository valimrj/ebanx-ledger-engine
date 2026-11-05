package com.ebanx.ledger

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.routing

/**
 * Ponto de entrada do processo. Eu só subo o servidor Ktor na porta configurada
 * aqui; nenhuma regra de negócio vive nesta classe.
 */
fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

/**
 * Composição da aplicação: plugins de transporte (JSON, mapeamento de erro) e
 * rotas. Eu adiciono as rotas de negócio história a história; aqui começa vazio
 * de propósito, pra não antecipar nada que ainda não foi pedido.
 */
fun Application.module() {
    install(ContentNegotiation) {
        json()
    }
    install(StatusPages)
    routing {
        // rotas de negócio chegam nas próximas histórias
    }
}
