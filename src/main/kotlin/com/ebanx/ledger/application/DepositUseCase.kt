package com.ebanx.ledger.application

import com.ebanx.ledger.domain.Account
import com.ebanx.ledger.domain.AccountId
import com.ebanx.ledger.infrastructure.AccountRepository


class DepositUseCase(private val repository: AccountRepository) {

    fun execute(destination: AccountId, amount: Int): Account =
        repository.update(destination) { existing ->
            existing?.deposit(amount) ?: Account.openWith(destination, initialDeposit = amount)
        }
}
