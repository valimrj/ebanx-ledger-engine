package com.ebanx.ledger.application

import com.ebanx.ledger.domain.Account
import com.ebanx.ledger.domain.AccountId
import com.ebanx.ledger.infrastructure.AccountRepository

/**
 * Single reason to change: the deposit rule. Deposit never fails in the
 * spec — that's why there's no validation `when`/`if` here, just the
 * happy path.
 *
 * I use `repository.update` (read + transform + write as one atomic
 * operation) instead of `findById` followed by a separate `save`, because
 * two concurrent deposits on the same new account, done in two steps,
 * could both read "doesn't exist" and both try to create the account —
 * one of the two deposits would get lost.
 */
class DepositUseCase(private val repository: AccountRepository) {

    fun execute(destination: AccountId, amount: Int): Account =
        repository.update(destination) { existing ->
            existing?.deposit(amount) ?: Account.openWith(destination, initialDeposit = amount)
        }
}
