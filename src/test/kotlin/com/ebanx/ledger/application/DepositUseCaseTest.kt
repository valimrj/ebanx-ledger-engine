package com.ebanx.ledger.application

import com.ebanx.ledger.domain.AccountId
import com.ebanx.ledger.infrastructure.InMemoryAccountRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class DepositUseCaseTest {

    private val repository = InMemoryAccountRepository()
    private val depositUseCase = DepositUseCase(repository)

    @Test
    fun `I deposit into a new account and it gets created with the deposit's balance`() {
        val account = depositUseCase.execute(destination = AccountId("100"), amount = 10)

        assertEquals(10, account.balance)
    }

    @Test
    fun `I deposit into an existing account and the balance adds to what it already had`() {
        depositUseCase.execute(destination = AccountId("100"), amount = 10)

        val account = depositUseCase.execute(destination = AccountId("100"), amount = 10)

        assertEquals(20, account.balance)
    }
}
