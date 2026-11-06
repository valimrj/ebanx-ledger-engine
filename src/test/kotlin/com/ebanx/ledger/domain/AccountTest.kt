package com.ebanx.ledger.domain

import com.ebanx.ledger.exceptions.InsufficientFundsException
import com.ebanx.ledger.exceptions.InvalidAmountException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AccountTest {

    @Test
    fun `I deposit and the balance adds up`() {
        val account = Account(id = AccountId("100"), balance = 10)

        val result = account.deposit(10)

        assertEquals(20, result.balance)
    }

    @Test
    fun `I deposit into a new account and the balance equals the deposited amount`() {
        val account = Account.openWith(id = AccountId("100"), initialDeposit = 10)

        assertEquals(10, account.balance)
        assertEquals(AccountId("100"), account.id)
    }

    @Test
    fun `I withdraw and the balance subtracts`() {
        val account = Account(id = AccountId("100"), balance = 20)

        val result = account.withdraw(5)

        assertEquals(15, result.balance)
    }

    @Test
    fun `deposit does not change the original account because Account is immutable`() {
        val account = Account(id = AccountId("100"), balance = 10)

        account.deposit(10)

        assertEquals(10, account.balance)
    }

    @Test
    fun `I withdraw exactly the available balance and the account ends up at zero`() {
        val account = Account(id = AccountId("100"), balance = 15)

        val result = account.withdraw(15)

        assertEquals(0, result.balance)
    }

    @Test
    fun `I try to withdraw more than the available balance and get InsufficientFundsException`() {
        val account = Account(id = AccountId("100"), balance = 10)

        val exception = assertFailsWith<InsufficientFundsException> {
            account.withdraw(11)
        }

        assertEquals(AccountId("100"), exception.accountId)
    }

    @Test
    fun `a withdraw that fails on insufficient funds does not change the original balance`() {
        val account = Account(id = AccountId("100"), balance = 10)

        runCatching { account.withdraw(11) }

        assertEquals(10, account.balance)
    }

    @Test
    fun `I try to deposit a zero or negative amount and get InvalidAmountException`() {
        val account = Account(id = AccountId("100"), balance = 10)

        assertFailsWith<InvalidAmountException> { account.deposit(0) }
        assertFailsWith<InvalidAmountException> { account.deposit(-5) }
    }

    @Test
    fun `I try to withdraw a zero or negative amount and get InvalidAmountException`() {
        val account = Account(id = AccountId("100"), balance = 10)

        assertFailsWith<InvalidAmountException> { account.withdraw(0) }
        assertFailsWith<InvalidAmountException> { account.withdraw(-5) }
    }

    @Test
    fun `a negative withdraw amount cannot behave like a deposit`() {
        val account = Account(id = AccountId("100"), balance = 10)

        runCatching { account.withdraw(-5) }

        assertEquals(10, account.balance)
    }

    @Test
    fun `I try to open an account with a zero or negative initial deposit and get InvalidAmountException`() {
        assertFailsWith<InvalidAmountException> { Account.openWith(AccountId("100"), initialDeposit = 0) }
        assertFailsWith<InvalidAmountException> { Account.openWith(AccountId("100"), initialDeposit = -5) }
    }
}
