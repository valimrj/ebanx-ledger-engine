package com.ebanx.ledger.application

import com.ebanx.ledger.domain.Account
import com.ebanx.ledger.domain.AccountId
import com.ebanx.ledger.infrastructure.AccountRepository

/**
 * Único motivo de mudar: orquestrar o saque — buscar a conta e persistir o
 * resultado. A regra "não pode sacar mais do que tem" eu não repito aqui:
 * ela já vive em `Account.withdraw`, que lança `InsufficientFundsException`
 * e deixa essa exceção propagar pra fora, sem eu precisar de um try/catch
 * só pra re-lançar a mesma coisa.
 */
class WithdrawUseCase(private val repository: AccountRepository) {

    fun execute(origin: AccountId, amount: Int): Account {
        val account = repository.findById(origin) ?: throw AccountNotFoundException(origin)

        return repository.save(account.withdraw(amount))
    }
}
