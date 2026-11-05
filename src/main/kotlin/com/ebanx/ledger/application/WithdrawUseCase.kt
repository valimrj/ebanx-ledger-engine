package com.ebanx.ledger.application

import com.ebanx.ledger.domain.Account
import com.ebanx.ledger.domain.AccountId
import com.ebanx.ledger.infrastructure.AccountRepository

/**
 * Único motivo de mudar: orquestrar o saque. A regra "não pode sacar mais
 * do que tem" eu não repito aqui: ela já vive em `Account.withdraw`, que
 * lança `InsufficientFundsException`.
 *
 * Eu uso `repository.update` em vez de `findById` seguido de um `save`
 * separado por duas razões, as duas críticas sob concorrência: (1) duas
 * threads lendo o mesmo saldo antigo e escrevendo por cima uma da outra
 * perderiam um saque (lost update); (2) sem essa atomicidade, duas threads
 * poderiam cada uma ver saldo suficiente *na leitura* e as duas saques
 * passarem, deixando a conta negativa — exatamente o bug que
 * `InsufficientFundsException` existe pra evitar. `ConcurrentHashMap.compute`
 * serializa as duas chamadas pra essa mesma conta, então a segunda só
 * decide depois que a primeira já terminou de escrever.
 */
class WithdrawUseCase(private val repository: AccountRepository) {

    fun execute(origin: AccountId, amount: Int): Account =
        repository.update(origin) { existing ->
            (existing ?: throw AccountNotFoundException(origin)).withdraw(amount)
        }
}
