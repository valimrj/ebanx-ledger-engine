package com.ebanx.ledger.infrastructure

import com.ebanx.ledger.domain.Account
import com.ebanx.ledger.domain.AccountId
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementação em memória da porta `AccountRepository` (mesmo pacote: a
 * única implementação que existe hoje mora junto da interface).
 *
 * `update` usa `ConcurrentHashMap.compute`, que o Java garante que executa
 * de forma atômica por chave: nenhuma outra thread vê um estado
 * intermediário, e duas chamadas concorrentes pra a MESMA conta são
 * serializadas (uma espera a outra), nunca correm em paralelo lendo o
 * mesmo saldo antigo. Isso é o que torna `withdraw`/`deposit` seguros sob
 * concorrência sem eu precisar de lock explícito nem de banco — suficiente
 * pro que o enunciado pede (durabilidade não é requisito, consistência é).
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
