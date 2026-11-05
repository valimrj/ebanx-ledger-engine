package com.ebanx.ledger.infrastructure

import com.ebanx.ledger.domain.Account
import com.ebanx.ledger.domain.AccountId
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
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
    fun `eu crio uma conta com update e recupero ela pelo id`() {
        val repository: AccountRepository = InMemoryAccountRepository()

        repository.update(AccountId("100")) { Account(AccountId("100"), balance = 10) }

        assertEquals(Account(AccountId("100"), 10), repository.findById(AccountId("100")))
    }

    @Test
    fun `eu busco uma conta que nao existe e recebo null`() {
        val repository: AccountRepository = InMemoryAccountRepository()

        assertNull(repository.findById(AccountId("nao-existe")))
    }

    @Test
    fun `eu limpo o repositorio e as contas somem`() {
        val repository: AccountRepository = InMemoryAccountRepository()
        repository.update(AccountId("100")) { Account(AccountId("100"), balance = 10) }

        repository.clear()

        assertNull(repository.findById(AccountId("100")))
    }

    /**
     * Esta é a prova de que a leitura-decisão-escrita é atômica, não só
     * "protegida pelo ConcurrentHashMap". Se `update` fizesse um
     * findById + save como duas operações separadas, 200 threads somando 1
     * concorrentemente perderiam incrementos (lost update) e o resultado
     * final seria menor que 200 — de forma não-determinística, o que
     * tornaria esse teste flaky exatamente pelo motivo certo.
     */
    @Test
    fun `200 updates concorrentes na mesma conta nao perdem nenhum incremento`() {
        val repository: AccountRepository = InMemoryAccountRepository()
        repository.update(AccountId("100")) { Account(AccountId("100"), balance = 0) }

        val threadCount = 200
        // Pool com uma thread por tarefa de propósito: todas precisam poder
        // esperar na barreira ao mesmo tempo. Um pool menor que threadCount
        // trava (as tarefas enfileiradas nunca chegam a decrementar
        // readyToStart, que o main thread espera chegar a zero antes de
        // soltar startSignal) — eu vi esse deadlock na primeira versão
        // deste teste.
        val executor = Executors.newFixedThreadPool(threadCount)
        val readyToStart = CountDownLatch(threadCount)
        val startSignal = CountDownLatch(1)
        val done = CountDownLatch(threadCount)

        repeat(threadCount) {
            executor.submit {
                readyToStart.countDown()
                startSignal.await()
                repository.update(AccountId("100")) { account -> account!!.deposit(1) }
                done.countDown()
            }
        }

        readyToStart.await()
        startSignal.countDown() // solta todas as threads de uma vez, maximizando a corrida
        done.await()
        executor.shutdown()

        assertEquals(200, repository.findById(AccountId("100"))?.balance)
    }

    /**
     * Prova o rollback automático: se a transformação lança uma exceção
     * (ex.: `InsufficientFundsException`), o estado anterior não pode ter
     * sido parcialmente sobrescrito.
     */
    @Test
    fun `update que lanca excecao nao altera o estado anterior`() {
        val repository: AccountRepository = InMemoryAccountRepository()
        repository.update(AccountId("100")) { Account(AccountId("100"), balance = 10) }

        runCatching {
            repository.update(AccountId("100")) { throw RuntimeException("falha de propósito") }
        }

        assertEquals(10, repository.findById(AccountId("100"))?.balance)
    }
}
