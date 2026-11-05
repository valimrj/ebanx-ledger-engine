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
 * ramificação extra — a única decisão que `withdraw` toma é a que protege o
 * invariante do domínio: saldo nunca fica negativo.
 *
 * Essa validação mora aqui, não no caso de uso, porque "saldo não pode ficar
 * negativo" é uma regra do que uma Account É, não uma regra de orquestração
 * — ela vale pra withdraw direto e pra transfer (que reusa withdraw), sem eu
 * precisar repetir o `if` em dois lugares.
 */
data class Account(
    val id: AccountId,
    val balance: Int,
) {
    fun deposit(amount: Int): Account = copy(balance = balance + amount)

    fun withdraw(amount: Int): Account {
        if (amount > balance) throw InsufficientFundsException(id)
        return copy(balance = balance - amount)
    }

    companion object {
        /** Abre uma conta nova já com o primeiro depósito. */
        fun openWith(id: AccountId, initialDeposit: Int): Account = Account(id, initialDeposit)
    }
}

/** Saque ou transferência pediram mais do que a conta tem disponível. */
class InsufficientFundsException(val accountId: AccountId) :
    RuntimeException("Account ${accountId.value} has insufficient funds")
