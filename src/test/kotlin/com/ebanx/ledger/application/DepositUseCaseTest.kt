package com.ebanx.ledger.application

import com.ebanx.ledger.domain.AccountId
import com.ebanx.ledger.infrastructure.InMemoryAccountRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class DepositUseCaseTest {

    private val repository = InMemoryAccountRepository()
    private val depositUseCase = DepositUseCase(repository)

    @Test
    fun `eu deposito numa conta nova e ela e criada com o saldo do deposito`() {
        val account = depositUseCase.execute(destination = AccountId("100"), amount = 10)

        assertEquals(10, account.balance)
    }

    @Test
    fun `eu deposito numa conta existente e o saldo soma ao que ja tinha`() {
        depositUseCase.execute(destination = AccountId("100"), amount = 10)

        val account = depositUseCase.execute(destination = AccountId("100"), amount = 10)

        assertEquals(20, account.balance)
    }
}
