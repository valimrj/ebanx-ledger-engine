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

    /**
     * Ponto conceitual que eu preciso provar, não só afirmar: GET não pode
     * ter efeito colateral. Eu chamo a consulta várias vezes e confirmo que
     * o saldo não muda nenhuma vez — se `execute` algum dia chamasse
     * `repository.save` por engano, esse teste quebraria.
     */
    @Test
    fun `eu consulto o saldo varias vezes e o saldo nunca muda so por consultar`() {
        DepositUseCase(repository).execute(AccountId("100"), amount = 20)

        repeat(5) {
            assertEquals(20, getBalanceUseCase.execute(AccountId("100"))?.balance)
        }

        assertEquals(20, repository.findById(AccountId("100"))?.balance)
    }
}
