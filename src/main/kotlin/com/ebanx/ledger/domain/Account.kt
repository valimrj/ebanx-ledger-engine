package com.ebanx.ledger.domain

import com.ebanx.ledger.exceptions.InsufficientFundsException
import com.ebanx.ledger.exceptions.InvalidAmountException

/**
 * Account identifier. I wrap the raw string in its own type so that an
 * "account id" never gets confused with any other string in the code.
 */
data class AccountId(val value: String)

/**
 * Account is an immutable domain object: every operation returns a new
 * instance instead of mutating the current state. That removes a whole
 * class of shared-state bugs and keeps the methods small, with no extra
 * branching - the only decision `withdraw` makes is the one that protects
 * the domain invariant: balance never goes negative.
 *
 * That validation lives here, not in the use case, because "balance can't
 * go negative" is a rule about what an Account IS, not an orchestration
 * rule - it applies to a direct withdraw and to transfer (which reuses
 * withdraw), without me repeating the `if` in two places.
 */
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

        // Amount validation lives here, not in the use case or the route:
        // it's a rule about what an Account operation means (there's no
        // such thing as a zero or negative deposit/withdraw), so it applies
        // to all three entry points (deposit, withdraw, openWith) without
        // me repeating the `if`.
        private fun requirePositive(amount: Int) {
            if (amount <= 0) throw InvalidAmountException(amount)
        }
    }
}
