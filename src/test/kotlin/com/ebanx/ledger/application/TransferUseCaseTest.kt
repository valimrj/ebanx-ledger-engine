package com.ebanx.ledger.application

import com.ebanx.ledger.domain.AccountId
import com.ebanx.ledger.exceptions.AccountNotFoundException
import com.ebanx.ledger.exceptions.InsufficientFundsException
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
    fun `I transfer from a non-existing origin and get AccountNotFoundException`() {
        assertFailsWith<AccountNotFoundException> {
            transferUseCase.execute(origin = AccountId("200"), destination = AccountId("300"), amount = 15)
        }
    }

    @Test
    fun `I transfer from an existing origin to a brand new destination`() {
        depositUseCase.execute(destination = AccountId("100"), amount = 15)

        val result = transferUseCase.execute(origin = AccountId("100"), destination = AccountId("300"), amount = 15)

        assertEquals(0, result.origin.balance)
        assertEquals(15, result.destination.balance)
    }

    @Test
    fun `I transfer from an existing origin to a destination that already exists`() {
        depositUseCase.execute(destination = AccountId("100"), amount = 15)
        depositUseCase.execute(destination = AccountId("300"), amount = 5)

        val result = transferUseCase.execute(origin = AccountId("100"), destination = AccountId("300"), amount = 15)

        assertEquals(0, result.origin.balance)
        assertEquals(20, result.destination.balance)
    }

    @Test
    fun `I transfer more than the origin has and the destination is left untouched`() {
        depositUseCase.execute(destination = AccountId("100"), amount = 10)

        assertFailsWith<InsufficientFundsException> {
            transferUseCase.execute(origin = AccountId("100"), destination = AccountId("300"), amount = 11)
        }

        assertEquals(10, repository.findById(AccountId("100"))?.balance)
        assertNull(repository.findById(AccountId("300")))
    }
}
