package com.ebanx.ledger.application

import com.ebanx.ledger.domain.AccountId
import com.ebanx.ledger.domain.InsufficientFundsException
import com.ebanx.ledger.infrastructure.InMemoryAccountRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WithdrawUseCaseTest {

    private val repository = InMemoryAccountRepository()
    private val depositUseCase = DepositUseCase(repository)
    private val withdrawUseCase = WithdrawUseCase(repository)

    @Test
    fun `eu saco de uma conta inexistente e recebo AccountNotFoundException`() {
        assertFailsWith<AccountNotFoundException> {
            withdrawUseCase.execute(origin = AccountId("200"), amount = 10)
        }
    }

    @Test
    fun `eu saco de uma conta existente e o saldo subtrai`() {
        depositUseCase.execute(destination = AccountId("100"), amount = 20)

        val account = withdrawUseCase.execute(origin = AccountId("100"), amount = 5)

        assertEquals(15, account.balance)
    }

    @Test
    fun `eu tento sacar mais do que o saldo e recebo InsufficientFundsException, sem alterar o saldo`() {
        depositUseCase.execute(destination = AccountId("100"), amount = 10)

        assertFailsWith<InsufficientFundsException> {
            withdrawUseCase.execute(origin = AccountId("100"), amount = 11)
        }

        assertEquals(10, repository.findById(AccountId("100"))?.balance)
    }
}
