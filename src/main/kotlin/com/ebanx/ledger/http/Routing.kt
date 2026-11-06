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
 * Dependency wiring and the business routes. It's kept out of
 * `Application.module()` on purpose, so module() stays "install transport
 * plugins" only - this file is "what the API does".
 *
 * Each handler only does two things - read the request and delegate to
 * the right use case - exactly to keep each route's cyclomatic complexity
 * at 1. The decision of "which use case to call" was already made before,
 * in EventDispatcher; error-to-status-code mapping is centralized here in
 * StatusPages, not repeated route by route.
 */

// The assignment's examples always show "0" as the plain-text body for
// every error response (404 and 400 alike), regardless of the real
// reason. Naming it instead of repeating the bare literal three times
// below makes that choice explicit, not something to guess at each call site.
private const val ERROR_RESPONSE_BODY = "0"

fun Application.businessRouting(repository: AccountRepository = InMemoryAccountRepository()) {
    val depositUseCase = DepositUseCase(repository)
    val withdrawUseCase = WithdrawUseCase(repository)
    val transferUseCase = TransferUseCase(withdrawUseCase, depositUseCase)
    val resetUseCase = ResetUseCase(repository)
    val getBalanceUseCase = GetBalanceUseCase(repository)
    val eventDispatcher = EventDispatcher(depositUseCase, withdrawUseCase, transferUseCase)

    install(StatusPages) {
        exception<AccountNotFoundException> { call, _ ->
            call.respondText(ERROR_RESPONSE_BODY, ContentType.Text.Plain, HttpStatusCode.NotFound)
        }
        // The assignment doesn't cover this case in its examples, so the
        // status choice (400, not 404 - the account exists, it just has no
        // funds) is mine, not dictated by the spec.
        exception<InsufficientFundsException> { call, _ ->
            call.respondText(ERROR_RESPONSE_BODY, ContentType.Text.Plain, HttpStatusCode.BadRequest)
        }
        // Same reasoning: a zero/negative amount is also invalid input,
        // not a 404 nor insufficient funds.
        exception<InvalidAmountException> { call, _ ->
            call.respondText(ERROR_RESPONSE_BODY, ContentType.Text.Plain, HttpStatusCode.BadRequest)
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
