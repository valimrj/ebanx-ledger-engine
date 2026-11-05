package com.ebanx.ledger.application

import com.ebanx.ledger.ports.AccountRepository

/** Único motivo de mudar: limpar o estado entre execuções do suite de testes. */
class ResetUseCase(private val repository: AccountRepository) {
    fun execute() = repository.clear()
}
