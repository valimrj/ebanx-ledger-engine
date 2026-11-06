package com.ebanx.ledger.exceptions

class InvalidAmountException(val amount: Int) :
    RuntimeException("Amount must be positive, got $amount")
