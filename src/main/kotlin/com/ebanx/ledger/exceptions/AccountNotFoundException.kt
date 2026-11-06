package com.ebanx.ledger.exceptions

import com.ebanx.ledger.domain.AccountId

/**
 * Signals that the requested account doesn't exist. Translating it to a
 * 404 is the HTTP adapter's sole responsibility.
 */
class AccountNotFoundException(val accountId: AccountId) : RuntimeException("Account ${accountId.value} not found")
