package com.ebanx.ledger.application

import com.ebanx.ledger.domain.Account
import com.ebanx.ledger.domain.AccountId
import com.ebanx.ledger.infrastructure.AccountRepository

/**
 * Único motivo de mudar: a regra de saque. Não valido saldo insuficiente de
 * propósito — o enunciado não testa esse caso, então não antecipo uma regra
 * que não foi pedida.
 */
class WithdrawUseCase(private val repository: AccountRepository) {

    fun execute(origin: AccountId, amount: Int): Account {
        val account = repository.findById(origin) ?: throw AccountNotFoundException(origin)

        return repository.save(account.withdraw(amount))
    }
}
