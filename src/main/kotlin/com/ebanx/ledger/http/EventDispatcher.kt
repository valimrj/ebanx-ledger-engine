package com.ebanx.ledger.http

import com.ebanx.ledger.application.DepositUseCase
import com.ebanx.ledger.application.TransferUseCase
import com.ebanx.ledger.application.WithdrawUseCase
import com.ebanx.ledger.domain.Account
import com.ebanx.ledger.domain.AccountId
import com.ebanx.ledger.http.EventType.DEPOSIT
import com.ebanx.ledger.http.EventType.TRANSFER
import com.ebanx.ledger.http.EventType.WITHDRAW

class EventDispatcher(
    private val depositUseCase: DepositUseCase,
    private val withdrawUseCase: WithdrawUseCase,
    private val transferUseCase: TransferUseCase,
) {

    fun dispatch(request: EventRequest): EventResponse = when (request.type) {
        DEPOSIT -> depositResponse(request)
        WITHDRAW -> withdrawResponse(request)
        TRANSFER -> transferResponse(request)
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

    private fun requireField(value: String?, fieldName: String): String =
        value ?: throw IllegalArgumentException("Missing field: $fieldName")
}

private fun Account.toView() = AccountView(id = id.value, balance = balance)
