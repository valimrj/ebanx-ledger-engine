package com.ebanx.ledger.infrastructure

import com.ebanx.ledger.domain.Account
import com.ebanx.ledger.domain.AccountId
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory implementation of the `AccountRepository` port (same package:
 * the only implementation that exists today lives next to the interface).
 *
 * `update` uses `ConcurrentHashMap.compute`, which Java guarantees runs
 * atomically per key: no other thread sees an intermediate state, and two
 * concurrent calls for the SAME account are serialized (one waits for the
 * other), never running in parallel reading the same stale balance. That's
 * what makes `withdraw`/`deposit` safe under concurrency without me
 * needing an explicit lock or a database - enough for what the assignment
 * asks for (durability isn't a requirement, consistency is).
 */
class InMemoryAccountRepository : AccountRepository {
    private val accounts = ConcurrentHashMap<AccountId, Account>()

    override fun findById(id: AccountId): Account? = accounts[id]

    override fun update(id: AccountId, mutate: (Account?) -> Account): Account =
        accounts.compute(id) { _, existing -> mutate(existing) }!!

    override fun clear() {
        accounts.clear()
    }
}
