package com.ebanx.ledger.infrastructure

import com.ebanx.ledger.domain.Account
import com.ebanx.ledger.domain.AccountId

/**
 * The domain's outbound port to account storage. I only put here the three
 * methods the use cases actually need - nothing speculative.
 *
 * This is the only abstraction I create before needing a second
 * implementation: the assignment already warns that it may ask for
 * changes, and swapping the in-memory storage for a database is the most
 * likely one.
 *
 * I keep this interface in the same file/package as its single
 * implementation (`InMemoryAccountRepository`, below) instead of a
 * separate `ports` package - one less folder to navigate, since today
 * there's only one implementation. `application` depends only on this
 * interface, never on the concrete class; that's the rule `LayeringTest`
 * checks.
 */
interface AccountRepository {
    fun findById(id: AccountId): Account?

    /**
     * Reads, transforms and writes as ONE atomic operation - not as
     * `findById` followed by a separate `save`. This matters because
     * `withdraw`/`deposit` do "read balance, decide, write balance", and
     * reading-deciding-writing in two steps is exactly where two
     * concurrent requests on the same account would lose an update (lost
     * update) even with a thread-safe data structure underneath. `mutate`
     * receives the current account (`null` if it doesn't exist) and
     * returns the new account; if `mutate` throws (e.g. insufficient
     * funds), nothing is written - the previous state stays intact.
     */
    fun update(id: AccountId, mutate: (Account?) -> Account): Account

    fun clear()
}
