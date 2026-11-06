package com.ebanx.ledger.infrastructure

import com.ebanx.ledger.domain.Account
import com.ebanx.ledger.domain.AccountId
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InMemoryAccountRepositoryTest {

    @Test
    fun `I create an account with update and get it back by id`() {
        val repository: AccountRepository = InMemoryAccountRepository()

        repository.update(AccountId("100")) { Account(AccountId("100"), balance = 10) }

        assertEquals(Account(AccountId("100"), 10), repository.findById(AccountId("100")))
    }

    @Test
    fun `I look up an account that doesn't exist and get null`() {
        val repository: AccountRepository = InMemoryAccountRepository()

        assertNull(repository.findById(AccountId("does-not-exist")))
    }

    @Test
    fun `I clear the repository and the accounts disappear`() {
        val repository: AccountRepository = InMemoryAccountRepository()
        repository.update(AccountId("100")) { Account(AccountId("100"), balance = 10) }

        repository.clear()

        assertNull(repository.findById(AccountId("100")))
    }

    @Test
    fun `200 concurrent updates on the same account lose no increment`() {
        val repository: AccountRepository = InMemoryAccountRepository()
        repository.update(AccountId("100")) { Account(AccountId("100"), balance = 0) }

        val threadCount = 200
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
        startSignal.countDown()
        done.await()
        executor.shutdown()

        assertEquals(200, repository.findById(AccountId("100"))?.balance)
    }

    @Test
    fun `an update that throws does not change the previous state`() {
        val repository: AccountRepository = InMemoryAccountRepository()
        repository.update(AccountId("100")) { Account(AccountId("100"), balance = 10) }

        runCatching {
            repository.update(AccountId("100")) { throw RuntimeException("failure on purpose") }
        }

        assertEquals(10, repository.findById(AccountId("100"))?.balance)
    }
}
