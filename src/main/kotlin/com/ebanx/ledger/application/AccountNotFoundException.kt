package com.ebanx.ledger.application

import com.ebanx.ledger.domain.AccountId

/**
 * Signals that the requested account doesn't exist. It lives in
 * `application` because it's a case the use cases need to communicate;
 * translating it to a 404 is the HTTP adapter's sole responsibility.
 */
class AccountNotFoundException(val accountId: AccountId) : RuntimeException("Account ${accountId.value} not found")
