package com.ebanx.ledger.exceptions

import com.ebanx.ledger.domain.AccountId

class InsufficientFundsException(val accountId: AccountId) :
    RuntimeException("Account ${accountId.value} has insufficient funds")
