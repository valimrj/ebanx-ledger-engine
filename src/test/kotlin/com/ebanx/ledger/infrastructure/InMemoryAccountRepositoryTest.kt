package com.ebanx.ledger.infrastructure

import com.ebanx.ledger.domain.Account
import com.ebanx.ledger.domain.AccountId
import com.ebanx.ledger.ports.AccountRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Eu testo a porta `AccountRepository` através da implementação em memória —
 * é a única implementação que existe agora, então não há motivo pra duplicar
 * os testes num fake separado.
 */
class InMemoryAccountRepositoryTest {

    @Test
    fun `eu salvo uma conta e recupero ela pelo id`() {
        val repository: AccountRepository = InMemoryAccountRepository()
        val account = Account(AccountId("100"), balance = 10)

        repository.save(account)

        assertEquals(account, repository.findById(AccountId("100")))
    }

    @Test
    fun `eu busco uma conta que nao existe e recebo null`() {
        val repository: AccountRepository = InMemoryAccountRepository()

        assertNull(repository.findById(AccountId("nao-existe")))
    }

    @Test
    fun `eu limpo o repositorio e as contas somem`() {
        val repository: AccountRepository = InMemoryAccountRepository()
        repository.save(Account(AccountId("100"), balance = 10))

        repository.clear()

        assertNull(repository.findById(AccountId("100")))
    }
}
