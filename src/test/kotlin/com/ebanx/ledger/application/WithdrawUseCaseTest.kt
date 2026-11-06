package com.ebanx.ledger.application

import com.ebanx.ledger.domain.AccountId
import com.ebanx.ledger.exceptions.AccountNotFoundException
import com.ebanx.ledger.exceptions.InsufficientFundsException
import com.ebanx.ledger.infrastructure.InMemoryAccountRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WithdrawUseCaseTest {

    private val repository = InMemoryAccountRepository()
    private val depositUseCase = DepositUseCase(repository)
    private val withdrawUseCase = WithdrawUseCase(repository)

    @Test
    fun `I withdraw from a non-existing account and get AccountNotFoundException`() {
        assertFailsWith<AccountNotFoundException> {
            withdrawUseCase.execute(origin = AccountId("200"), amount = 10)
        }
    }

    @Test
    fun `I withdraw from an existing account and the balance subtracts`() {
        depositUseCase.execute(destination = AccountId("100"), amount = 20)

        val account = withdrawUseCase.execute(origin = AccountId("100"), amount = 5)

        assertEquals(15, account.balance)
    }

    @Test
    fun `I try to withdraw more than the balance and get InsufficientFundsException, without changing the balance`() {
        depositUseCase.execute(destination = AccountId("100"), amount = 10)

        assertFailsWith<InsufficientFundsException> {
            withdrawUseCase.execute(origin = AccountId("100"), amount = 11)
        }

        assertEquals(10, repository.findById(AccountId("100"))?.balance)
    }
}
