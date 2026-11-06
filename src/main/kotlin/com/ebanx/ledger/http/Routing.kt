package com.ebanx.ledger.http

import com.ebanx.ledger.application.DepositUseCase
import com.ebanx.ledger.application.GetBalanceUseCase
import com.ebanx.ledger.application.ResetUseCase
import com.ebanx.ledger.application.TransferUseCase
import com.ebanx.ledger.application.WithdrawUseCase
import com.ebanx.ledger.domain.AccountId
import com.ebanx.ledger.exceptions.AccountNotFoundException
import com.ebanx.ledger.exceptions.InsufficientFundsException
import com.ebanx.ledger.exceptions.InvalidAmountException
import com.ebanx.ledger.infrastructure.InMemoryAccountRepository
import com.ebanx.ledger.infrastructure.AccountRepository
import io.ktor.http.ContentType
import io.ktor.http.ContentType.Text
import io.ktor.http.ContentType.Text.Plain
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.NotFound
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

private const val ERROR_RESPONSE_BODY = "0"

fun Application.businessRouting(repository: AccountRepository = InMemoryAccountRepository()) {
    val depositUseCase = DepositUseCase(repository)
    val withdrawUseCase = WithdrawUseCase(repository)
    val transferUseCase = TransferUseCase(withdrawUseCase, depositUseCase)
    val resetUseCase = ResetUseCase(repository)
    val getBalanceUseCase = GetBalanceUseCase(repository)
    val eventDispatcher = EventDispatcher(depositUseCase, withdrawUseCase, transferUseCase)

    install(StatusPages) {
        exception<AccountNotFoundException> { call, _ -> call.respondText(ERROR_RESPONSE_BODY, Plain, NotFound) }
        exception<InsufficientFundsException> { call, _ -> call.respondText(ERROR_RESPONSE_BODY, Plain, BadRequest) }
        exception<InvalidAmountException> { call, _ -> call.respondText(ERROR_RESPONSE_BODY, Plain, BadRequest) }
    }

    routing {
        post("/event") {
            val response = eventDispatcher.dispatch(call.receive())
            call.respond(HttpStatusCode.Created, response)
        }

        get("/balance") {
            val accountId = AccountId(call.request.queryParameters.requireValue("account_id"))
            val account = getBalanceUseCase.execute(accountId) ?: throw AccountNotFoundException(accountId)

            call.respondText(account.balance.toString(), Plain, HttpStatusCode.OK)
        }

        post("/reset") {
            resetUseCase.execute()
            call.respondText("OK", Plain, HttpStatusCode.OK)
        }
    }
}

private fun Parameters.requireValue(name: String): String =
    this[name] ?: throw IllegalArgumentException("Missing query parameter: $name")
