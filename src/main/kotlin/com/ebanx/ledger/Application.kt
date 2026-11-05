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
 * Ponto de entrada do processo. Eu só subo o servidor Ktor na porta configurada
 * aqui; nenhuma regra de negócio vive nesta classe.
 */
fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

/**
 * Composição da aplicação: plugins de transporte (JSON) e as rotas de
 * negócio, que vivem em `http.businessRouting` — ela também instala o
 * `StatusPages` que mapeia exceção de domínio pra status HTTP.
 *
 * `explicitNulls = false` faz o serializer omitir campos nulos, porque o
 * enunciado só inclui a chave "origin" ou "destination" quando ela se aplica
 * ao tipo de evento.
 */
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
fun Application.module() {
    install(ContentNegotiation) {
        json(Json { explicitNulls = false })
    }
    businessRouting()
}
