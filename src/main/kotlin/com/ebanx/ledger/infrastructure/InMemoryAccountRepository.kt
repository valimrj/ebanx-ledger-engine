package com.ebanx.ledger.infrastructure

import com.ebanx.ledger.domain.Account
import com.ebanx.ledger.domain.AccountId
import com.ebanx.ledger.ports.AccountRepository
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementação em memória da porta `AccountRepository`. `ConcurrentHashMap`
 * já me dá segurança em acesso concorrente sem eu precisar de lock explícito
 * nem de banco — suficiente pro que o enunciado pede (durabilidade não é
 * requisito).
 */
class InMemoryAccountRepository : AccountRepository {
    private val accounts = ConcurrentHashMap<AccountId, Account>()

    override fun findById(id: AccountId): Account? = accounts[id]

    override fun save(account: Account): Account {
        accounts[account.id] = account
        return account
    }

    override fun clear() {
        accounts.clear()
    }
}
