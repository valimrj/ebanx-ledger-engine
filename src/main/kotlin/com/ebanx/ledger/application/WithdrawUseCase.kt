package com.ebanx.ledger.application

import com.ebanx.ledger.domain.Account
import com.ebanx.ledger.domain.AccountId
import com.ebanx.ledger.exceptions.AccountNotFoundException
import com.ebanx.ledger.infrastructure.AccountRepository

class WithdrawUseCase(private val repository: AccountRepository) {

    fun execute(origin: AccountId, amount: Int): Account =
        repository.update(origin) { existing ->
            (existing ?: throw AccountNotFoundException(origin)).withdraw(amount)
        }
}
