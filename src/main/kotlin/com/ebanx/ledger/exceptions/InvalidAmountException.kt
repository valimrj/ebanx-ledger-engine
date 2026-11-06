package com.ebanx.ledger.exceptions

/**
 * Zero or negative deposit/withdraw amount. Without this, a negative
 * withdraw would pass the sufficient-funds check (`amount > balance` is
 * false for a negative amount) and behave like an unlimited deposit -
 * exactly the kind of "invalid value not handled" that leaves the state
 * inconsistent.
 */
class InvalidAmountException(val amount: Int) :
    RuntimeException("Amount must be positive, got $amount")
