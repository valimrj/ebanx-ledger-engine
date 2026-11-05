package com.ebanx.ledger.http

import com.ebanx.ledger.application.AccountNotFoundException
import com.ebanx.ledger.application.DepositUseCase
import com.ebanx.ledger.application.GetBalanceUseCase
import com.ebanx.ledger.application.ResetUseCase
import com.ebanx.ledger.application.TransferUseCase
import com.ebanx.ledger.application.WithdrawUseCase
import com.ebanx.ledger.domain.AccountId
import com.ebanx.ledger.infrastructure.InMemoryAccountRepository
import com.ebanx.ledger.ports.AccountRepository
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

/**
 * Composição das dependências e das rotas de negócio. Fica fora de
 * `Application.module()` de propósito, pra module() continuar sendo só
 * "instalar plugins de transporte" — este arquivo é "o que a API faz".
 *
 * Cada handler só faz duas coisas — ler a requisição e delegar pro caso de
 * uso certo — exatamente pra manter a complexidade ciclomática de cada rota
 * em 1. A decisão de "qual caso de uso chamar" já foi tomada antes, no
 * EventDispatcher; o mapeamento de erro pra status code é centralizado aqui
 * no StatusPages, não repetido rota a rota.
 */
fun Application.businessRouting(repository: AccountRepository = InMemoryAccountRepository()) {
    val depositUseCase = DepositUseCase(repository)
    val withdrawUseCase = WithdrawUseCase(repository)
    val transferUseCase = TransferUseCase(withdrawUseCase, depositUseCase)
    val resetUseCase = ResetUseCase(repository)
    val getBalanceUseCase = GetBalanceUseCase(repository)
    val eventDispatcher = EventDispatcher(depositUseCase, withdrawUseCase, transferUseCase)

    install(StatusPages) {
        exception<AccountNotFoundException> { call, _ ->
            call.respondText("0", ContentType.Text.Plain, HttpStatusCode.NotFound)
        }
    }

    routing {
        post("/event") {
            val response = eventDispatcher.dispatch(call.receive())
            call.respond(HttpStatusCode.Created, response)
        }

        get("/balance") {
            val accountId = AccountId(call.request.queryParameters.requireValue("account_id"))
            val account = getBalanceUseCase.execute(accountId) ?: throw AccountNotFoundException(accountId)

            call.respondText(account.balance.toString(), ContentType.Text.Plain, HttpStatusCode.OK)
        }

        post("/reset") {
            resetUseCase.execute()
            call.respondText("OK", ContentType.Text.Plain, HttpStatusCode.OK)
        }
    }
}

private fun Parameters.requireValue(name: String): String =
    this[name] ?: throw IllegalArgumentException("Missing query parameter: $name")
