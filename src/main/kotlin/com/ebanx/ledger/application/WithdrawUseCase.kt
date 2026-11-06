package com.ebanx.ledger.application

import com.ebanx.ledger.domain.Account
import com.ebanx.ledger.domain.AccountId
import com.ebanx.ledger.infrastructure.AccountRepository

/**
 * Single reason to change: orchestrating the withdraw. The rule "can't
 * withdraw more than you have" isn't repeated here: it already lives in
 * `Account.withdraw`, which throws `InsufficientFundsException`.
 *
 * I use `repository.update` instead of `findById` followed by a separate
 * `save` for two reasons, both critical under concurrency: (1) two threads
 * reading the same stale balance and overwriting each other would lose a
 * withdraw (lost update); (2) without this atomicity, two threads could
 * each see sufficient funds *at read time* and both withdraws go through,
 * leaving the account negative — exactly the bug `InsufficientFundsException`
 * exists to prevent. `ConcurrentHashMap.compute` serializes the two calls
 * for that same account, so the second one only decides after the first
 * has already finished writing.
 */
class WithdrawUseCase(private val repository: AccountRepository) {

    fun execute(origin: AccountId, amount: Int): Account =
        repository.update(origin) { existing ->
            (existing ?: throw AccountNotFoundException(origin)).withdraw(amount)
        }
}
