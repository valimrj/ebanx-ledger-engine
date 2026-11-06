package com.ebanx.ledger.infrastructure

import com.ebanx.ledger.domain.Account
import com.ebanx.ledger.domain.AccountId
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * I test the `AccountRepository` port through the in-memory implementation -
 * it's the only implementation that exists right now, so there's no reason
 * to duplicate the tests on a separate fake.
 */
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

    /**
     * This is the proof that read-decide-write is atomic, not just
     * "protected by ConcurrentHashMap". If `update` did a findById + save
     * as two separate operations, 200 threads concurrently adding 1 would
     * lose increments (lost update) and the final result would be less
     * than 200 - non-deterministically, which would make this test flaky
     * for exactly the right reason.
     */
    @Test
    fun `200 concurrent updates on the same account lose no increment`() {
        val repository: AccountRepository = InMemoryAccountRepository()
        repository.update(AccountId("100")) { Account(AccountId("100"), balance = 0) }

        val threadCount = 200
        // Pool with one thread per task on purpose: all of them need to be
        // able to wait at the barrier at the same time. A pool smaller
        // than threadCount deadlocks (the queued tasks never get to
        // decrement readyToStart, which the main thread waits to reach
        // zero before releasing startSignal) - I saw this deadlock in the
        // first version of this test.
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
        startSignal.countDown() // releases all threads at once, maximizing the race
        done.await()
        executor.shutdown()

        assertEquals(200, repository.findById(AccountId("100"))?.balance)
    }

    /**
     * Proves the automatic rollback: if the transformation throws (e.g.
     * `InsufficientFundsException`), the previous state must not have
     * been partially overwritten.
     */
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
