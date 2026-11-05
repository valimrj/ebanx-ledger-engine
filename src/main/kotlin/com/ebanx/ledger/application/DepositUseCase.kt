package com.ebanx.ledger.application

import com.ebanx.ledger.domain.Account
import com.ebanx.ledger.domain.AccountId
import com.ebanx.ledger.infrastructure.AccountRepository

/**
 * Único motivo de mudar: a regra de depósito. Deposit nunca falha no spec —
 * por isso não há `when`/`if` de validação aqui, só o caminho feliz.
 *
 * Eu uso `repository.update` (ler + transformar + gravar numa operação
 * atômica) em vez de `findById` seguido de um `save` separado, porque dois
 * depósitos concorrentes na mesma conta nova, feitos em dois passos,
 * poderiam ambos ler "não existe" e ambos tentar criar a conta — um dos
 * dois depósitos se perderia.
 */
class DepositUseCase(private val repository: AccountRepository) {

    fun execute(destination: AccountId, amount: Int): Account =
        repository.update(destination) { existing ->
            existing?.deposit(amount) ?: Account.openWith(destination, initialDeposit = amount)
        }
}
