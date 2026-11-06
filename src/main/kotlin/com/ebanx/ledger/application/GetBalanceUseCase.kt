package com.ebanx.ledger.application

import com.ebanx.ledger.domain.Account
import com.ebanx.ledger.domain.AccountId
import com.ebanx.ledger.infrastructure.AccountRepository

/** Single reason to change: querying the balance. Null when the account doesn't exist. */
class GetBalanceUseCase(private val repository: AccountRepository) {
    fun execute(accountId: AccountId): Account? = repository.findById(accountId)
}
