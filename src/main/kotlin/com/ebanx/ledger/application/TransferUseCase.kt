package com.ebanx.ledger.application

import com.ebanx.ledger.domain.Account
import com.ebanx.ledger.domain.AccountId

data class TransferResult(val origin: Account, val destination: Account)

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
