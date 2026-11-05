package com.ebanx.ledger.application

import com.ebanx.ledger.domain.Account
import com.ebanx.ledger.domain.AccountId
import com.ebanx.ledger.infrastructure.AccountRepository

/**
 * Único motivo de mudar: a regra de depósito. Deposit nunca falha no spec —
 * por isso não há `when`/`if` de validação aqui, só o caminho feliz.
 */
class DepositUseCase(private val repository: AccountRepository) {

    fun execute(destination: AccountId, amount: Int): Account {
        val account = repository.findById(destination)
            ?.deposit(amount)
            ?: Account.openWith(destination, initialDeposit = amount)

        return repository.save(account)
    }
}
