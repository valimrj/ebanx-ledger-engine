package com.ebanx.ledger.application

import com.ebanx.ledger.domain.AccountId
import com.ebanx.ledger.infrastructure.InMemoryAccountRepository
import kotlin.test.Test
import kotlin.test.assertNull

class ResetUseCaseTest {

    @Test
    fun `eu dou reset e as contas somem do repositorio`() {
        val repository = InMemoryAccountRepository()
        DepositUseCase(repository).execute(AccountId("100"), amount = 10)

        ResetUseCase(repository).execute()

        assertNull(repository.findById(AccountId("100")))
    }
}
