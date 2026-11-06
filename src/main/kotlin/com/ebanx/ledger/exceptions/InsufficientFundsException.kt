package com.ebanx.ledger.exceptions

import com.ebanx.ledger.domain.AccountId

/** A withdraw or transfer asked for more than the account has available. */
class InsufficientFundsException(val accountId: AccountId) :
    RuntimeException("Account ${accountId.value} has insufficient funds")
