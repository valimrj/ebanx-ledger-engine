package com.ebanx.ledger.application

import com.ebanx.ledger.domain.AccountId
import com.ebanx.ledger.infrastructure.InMemoryAccountRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetBalanceUseCaseTest {

    private val repository = InMemoryAccountRepository()
    private val getBalanceUseCase = GetBalanceUseCase(repository)

    @Test
    fun `I look up the balance of a non-existing account and get null`() {
        assertNull(getBalanceUseCase.execute(AccountId("1234")))
    }

    @Test
    fun `I look up the balance of an existing account`() {
        DepositUseCase(repository).execute(AccountId("100"), amount = 20)

        assertEquals(20, getBalanceUseCase.execute(AccountId("100"))?.balance)
    }

    @Test
    fun `I query the balance several times and it never changes just by querying`() {
        DepositUseCase(repository).execute(AccountId("100"), amount = 20)

        repeat(5) {
            assertEquals(20, getBalanceUseCase.execute(AccountId("100"))?.balance)
        }

        assertEquals(20, repository.findById(AccountId("100"))?.balance)
    }
}
