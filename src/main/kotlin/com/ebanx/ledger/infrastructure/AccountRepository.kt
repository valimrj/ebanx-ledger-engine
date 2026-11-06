package com.ebanx.ledger.infrastructure

import com.ebanx.ledger.domain.Account
import com.ebanx.ledger.domain.AccountId

interface AccountRepository {
    fun findById(id: AccountId): Account?
    fun update(id: AccountId, mutate: (Account?) -> Account): Account
    fun clear()
}
