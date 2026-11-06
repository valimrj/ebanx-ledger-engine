package com.ebanx.ledger.application

import com.ebanx.ledger.domain.Account
import com.ebanx.ledger.domain.AccountId

/** Result of a transfer: the two final balances, origin and destination. */
data class TransferResult(val origin: Account, val destination: Account)

/**
 * Single reason to change: the transfer rule. I reuse Withdraw and Deposit
 * through composition instead of duplicating each one's logic - if the
 * origin account doesn't exist, the 404 already comes from inside
 * WithdrawUseCase, I just let the exception propagate.
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
