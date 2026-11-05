package com.ebanx.ledger.application

import com.ebanx.ledger.domain.Account
import com.ebanx.ledger.domain.AccountId
import com.ebanx.ledger.infrastructure.AccountRepository

/** Único motivo de mudar: consultar saldo. Não existe quando a conta não existe. */
class GetBalanceUseCase(private val repository: AccountRepository) {
    fun execute(accountId: AccountId): Account? = repository.findById(accountId)
}
