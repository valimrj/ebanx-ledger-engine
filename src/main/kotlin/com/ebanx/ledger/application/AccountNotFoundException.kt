package com.ebanx.ledger.application

import com.ebanx.ledger.domain.AccountId

/**
 * Sinaliza que a conta pedida não existe. Fica em `application` porque é um
 * caso que os casos de uso precisam comunicar; a tradução dela pra 404 é
 * responsabilidade exclusiva do adapter HTTP.
 */
class AccountNotFoundException(val accountId: AccountId) : RuntimeException("Account ${accountId.value} not found")
