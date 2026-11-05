package com.ebanx.ledger.application

import com.ebanx.ledger.domain.Account
import com.ebanx.ledger.domain.AccountId

/** Resultado de uma transferência: os dois saldos finais, origem e destino. */
data class TransferResult(val origin: Account, val destination: Account)

/**
 * Único motivo de mudar: a regra de transferência. Eu reuso Withdraw e
 * Deposit por composição em vez de duplicar a lógica de cada um — se a
 * conta de origem não existir, o 404 já vem de dentro de WithdrawUseCase,
 * eu só deixo a exceção propagar.
 */
class TransferUseCase(
    private val withdrawUseCase: WithdrawUseCase,
    private val depositUseCase: DepositUseCase,
) {

    fun execute(origin: AccountId, destination: AccountId, amount: Int): TransferResult {
        val originAccount = withdrawUseCase.execute(origin, amount)
        val destinationAccount = depositUseCase.execute(destination, amount)

        return TransferResult(originAccount, destinationAccount)
    }
}
