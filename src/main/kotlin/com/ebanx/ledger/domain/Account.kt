package com.ebanx.ledger.domain

import com.ebanx.ledger.exceptions.InsufficientFundsException
import com.ebanx.ledger.exceptions.InvalidAmountException

/**
 * Account identifier. I wrap the raw string in its own type so that an
 * "account id" never gets confused with any other string in the code.
 */
data class AccountId(val value: String)

data class Account(
    val id: AccountId,
    val balance: Int,
) {
    fun deposit(amount: Int): Account {
        requirePositive(amount)
        return copy(balance = balance + amount)
    }

    fun withdraw(amount: Int): Account {
        requirePositive(amount)
        if (amount > balance) throw InsufficientFundsException(id)
        return copy(balance = balance - amount)
    }

    companion object {
        /** Opens a new account already with its first deposit. */
        fun openWith(id: AccountId, initialDeposit: Int): Account {
            requirePositive(initialDeposit)
            return Account(id, initialDeposit)
        }

        private fun requirePositive(amount: Int) {
            if (amount <= 0) throw InvalidAmountException(amount)
        }
    }
}
