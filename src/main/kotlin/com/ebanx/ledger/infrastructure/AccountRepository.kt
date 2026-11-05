package com.ebanx.ledger.infrastructure

import com.ebanx.ledger.domain.Account
import com.ebanx.ledger.domain.AccountId

/**
 * Porta de saída do domínio pro armazenamento de contas. Eu só coloco aqui os
 * três métodos que os casos de uso realmente precisam — nada especulativo.
 *
 * Esta é a única abstração que eu crio antes de precisar de uma segunda
 * implementação: o enunciado já avisa que pode pedir mudanças, e trocar o
 * armazenamento em memória por um banco é a mudança mais provável.
 *
 * Eu deixo essa interface no mesmo arquivo/pacote da sua única
 * implementação (`InMemoryAccountRepository`, embaixo) em vez de um pacote
 * `ports` separado — uma pasta a menos pra navegar, já que hoje só existe
 * uma implementação. `application` depende só desta interface, nunca da
 * classe concreta; é essa regra que o `LayeringTest` verifica.
 */
interface AccountRepository {
    fun findById(id: AccountId): Account?

    fun save(account: Account): Account

    fun clear()
}
