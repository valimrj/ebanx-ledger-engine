package com.ebanx.ledger.application

import com.ebanx.ledger.infrastructure.AccountRepository

class ResetUseCase(private val repository: AccountRepository) {
    fun execute() = repository.clear()
}
