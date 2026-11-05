package com.ebanx.ledger.application

import com.ebanx.ledger.domain.AccountId
import com.ebanx.ledger.domain.InsufficientFundsException
import com.ebanx.ledger.infrastructure.InMemoryAccountRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class TransferUseCaseTest {

    private val repository = InMemoryAccountRepository()
    private val depositUseCase = DepositUseCase(repository)
    private val transferUseCase = TransferUseCase(
        withdrawUseCase = WithdrawUseCase(repository),
        depositUseCase = depositUseCase,
    )

    @Test
    fun `eu transfiro de uma origem inexistente e recebo AccountNotFoundException`() {
        assertFailsWith<AccountNotFoundException> {
            transferUseCase.execute(origin = AccountId("200"), destination = AccountId("300"), amount = 15)
        }
    }

    @Test
    fun `eu transfiro de uma origem existente para um destino novo`() {
        depositUseCase.execute(destination = AccountId("100"), amount = 15)

        val result = transferUseCase.execute(origin = AccountId("100"), destination = AccountId("300"), amount = 15)

        assertEquals(0, result.origin.balance)
        assertEquals(15, result.destination.balance)
    }

    @Test
    fun `eu transfiro de uma origem existente para um destino que ja existe`() {
        depositUseCase.execute(destination = AccountId("100"), amount = 15)
        depositUseCase.execute(destination = AccountId("300"), amount = 5)

        val result = transferUseCase.execute(origin = AccountId("100"), destination = AccountId("300"), amount = 15)

        assertEquals(0, result.origin.balance)
        assertEquals(20, result.destination.balance)
    }

    @Test
    fun `eu transfiro mais do que a origem tem e o destino nao e tocado`() {
        depositUseCase.execute(destination = AccountId("100"), amount = 10)

        assertFailsWith<InsufficientFundsException> {
            transferUseCase.execute(origin = AccountId("100"), destination = AccountId("300"), amount = 11)
        }

        assertEquals(10, repository.findById(AccountId("100"))?.balance)
        assertNull(repository.findById(AccountId("300")))
    }
}
