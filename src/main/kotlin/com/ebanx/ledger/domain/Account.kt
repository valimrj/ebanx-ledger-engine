package com.ebanx.ledger.domain

/**
 * Identificador de conta. Eu encapsulo a string crua num tipo próprio pra não
 * confundir "id de conta" com qualquer outra string pelo código.
 */
data class AccountId(val value: String)

/**
 * Conta é um objeto de domínio imutável: cada operação devolve uma nova
 * instância em vez de mutar o estado atual. Isso elimina uma classe inteira
 * de bugs de estado compartilhado e mantém os métodos pequenos e sem
 * ramificação — complexidade ciclomática 1 em cada um deles.
 *
 * Eu não valido saldo negativo aqui de propósito: o enunciado não cobre esse
 * caso nos exemplos dados, então não anticipo uma regra que não foi pedida.
 */
data class Account(
    val id: AccountId,
    val balance: Int,
) {
    fun deposit(amount: Int): Account = copy(balance = balance + amount)

    fun withdraw(amount: Int): Account = copy(balance = balance - amount)

    companion object {
        /** Abre uma conta nova já com o primeiro depósito. */
        fun openWith(id: AccountId, initialDeposit: Int): Account = Account(id, initialDeposit)
    }
}
