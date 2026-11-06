package com.ebanx.ledger.application

import com.ebanx.ledger.infrastructure.AccountRepository

/** Single reason to change: clearing the state between test suite runs. */
class ResetUseCase(private val repository: AccountRepository) {
    fun execute() = repository.clear()
}
