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
        // O enunciado só define esses três tipos; não crio uma exceção de
        // domínio pra um caso que ele não descreve — isso seria antecipar
        // cenário fora da especificação. `IllegalArgumentException` já diz
        // o que precisa dizer: a entrada está fora do que a API aceita.
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

    // Mesmo raciocínio: campo obrigatório ausente pro tipo de evento também
    // não está nos exemplos do enunciado. IllegalArgumentException basta.
    private fun requireField(value: String?, fieldName: String): String =
        value ?: throw IllegalArgumentException("Missing field: $fieldName")
}

private fun Account.toView() = AccountView(id = id.value, balance = balance)
