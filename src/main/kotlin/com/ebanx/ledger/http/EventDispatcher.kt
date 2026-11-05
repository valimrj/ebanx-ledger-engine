package com.ebanx.ledger.http

import com.ebanx.ledger.application.DepositUseCase
import com.ebanx.ledger.application.TransferUseCase
import com.ebanx.ledger.application.WithdrawUseCase
import com.ebanx.ledger.domain.Account
import com.ebanx.ledger.domain.AccountId

/**
 * Único motivo de mudar: traduzir um `EventRequest` pro caso de uso certo e
 * devolver a forma de resposta que o enunciado espera. Fica no pacote http
 * porque EventRequest/EventResponse são formatos de fio, não conceitos de
 * domínio.
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
        else -> throw UnknownEventTypeException(request.type)
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
        value ?: throw MissingEventFieldException(fieldName)
}

private fun Account.toView() = AccountView(id = id.value, balance = balance)

/** Campo obrigatório pro tipo de evento não veio no corpo da requisição. */
class MissingEventFieldException(val fieldName: String) : RuntimeException("Missing field: $fieldName")

/** `type` recebido não é um dos três que o enunciado define. */
class UnknownEventTypeException(val type: String) : RuntimeException("Unknown event type: $type")
