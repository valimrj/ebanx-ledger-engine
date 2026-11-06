package com.ebanx.ledger.http

import com.ebanx.ledger.application.DepositUseCase
import com.ebanx.ledger.application.TransferUseCase
import com.ebanx.ledger.application.WithdrawUseCase
import com.ebanx.ledger.domain.Account
import com.ebanx.ledger.domain.AccountId

/**
 * Single reason to change: translating an `EventRequest` into the right
 * use case and returning the response shape the assignment expects. It
 * lives in the http package because EventRequest/EventResponse are wire
 * formats, not domain concepts.
 */
class EventDispatcher(
    private val depositUseCase: DepositUseCase,
    private val withdrawUseCase: WithdrawUseCase,
    private val transferUseCase: TransferUseCase,
) {

    fun dispatch(request: EventRequest): EventResponse = when (request.type) {
        "deposit" -> depositResponse(request)
        "withdraw" -> withdrawResponse(request)
        "transfer" -> transferResponse(request)
        // The assignment only defines these three types; I don't create a
        // domain exception for a case it doesn't describe — that would be
        // anticipating a scenario outside the spec. `IllegalArgumentException`
        // already says what it needs to say: the input is outside what the
        // API accepts.
        else -> throw IllegalArgumentException("Unknown event type: ${request.type}")
    }

    private fun depositResponse(request: EventRequest): EventResponse {
        val account = depositUseCase.execute(AccountId(requireField(request.destination, "destination")), request.amount)
        return EventResponse(destination = account.toView())
    }

    private fun withdrawResponse(request: EventRequest): EventResponse {
        val account = withdrawUseCase.execute(AccountId(requireField(request.origin, "origin")), request.amount)
        return EventResponse(origin = account.toView())
    }

    private fun transferResponse(request: EventRequest): EventResponse {
        val result = transferUseCase.execute(
            origin = AccountId(requireField(request.origin, "origin")),
            destination = AccountId(requireField(request.destination, "destination")),
            amount = request.amount,
        )
        return EventResponse(origin = result.origin.toView(), destination = result.destination.toView())
    }

    // Same reasoning: a required field missing for the event type is also
    // outside the assignment's examples. IllegalArgumentException is enough.
    private fun requireField(value: String?, fieldName: String): String =
        value ?: throw IllegalArgumentException("Missing field: $fieldName")
}

private fun Account.toView() = AccountView(id = id.value, balance = balance)
