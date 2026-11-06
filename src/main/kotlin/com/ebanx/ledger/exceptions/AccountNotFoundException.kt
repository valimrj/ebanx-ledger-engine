package com.ebanx.ledger.exceptions

import com.ebanx.ledger.domain.AccountId

class AccountNotFoundException(val accountId: AccountId) : RuntimeException("Account ${accountId.value} not found")
