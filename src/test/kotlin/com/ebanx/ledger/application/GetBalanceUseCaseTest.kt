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
    fun `eu busco o saldo de uma conta inexistente e recebo null`() {
        assertNull(getBalanceUseCase.execute(AccountId("1234")))
    }

    @Test
    fun `eu busco o saldo de uma conta existente`() {
        DepositUseCase(repository).execute(AccountId("100"), amount = 20)

        assertEquals(20, getBalanceUseCase.execute(AccountId("100"))?.balance)
    }
}
