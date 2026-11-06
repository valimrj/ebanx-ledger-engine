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

    // `request.type` is `EventType`, a closed enum, so this `when` is
    // exhaustive without an `else` branch - the compiler guarantees every
    // event kind is handled, and an unknown `type` in the JSON now fails
    // at deserialization, before dispatch is even called, instead of
    // needing a speculative "unknown type" branch here.
    fun dispatch(request: EventRequest): EventResponse = when (request.type) {
        EventType.DEPOSIT -> depositResponse(request)
        EventType.WITHDRAW -> withdrawResponse(request)
        EventType.TRANSFER -> transferResponse(request)
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

    // `fieldName` here is just a label for the error message, not a
    // business value with a closed set of options like `EventType` - the
    // actual values of `origin`/`destination` are arbitrary account ids,
    // so there's nothing to turn into an enum. A missing required field
    // is also outside the assignment's examples; IllegalArgumentException
    // is enough.
    private fun requireField(value: String?, fieldName: String): String =
        value ?: throw IllegalArgumentException("Missing field: $fieldName")
}

private fun Account.toView() = AccountView(id = id.value, balance = balance)
