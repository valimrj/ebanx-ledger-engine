package com.ebanx.ledger.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Eu testo a classe de domínio isolada, sem Ktor, sem repositório — só regra
 * de negócio pura.
 */
class AccountTest {

    @Test
    fun `eu deposito e o saldo soma`() {
        val account = Account(id = AccountId("100"), balance = 10)

        val result = account.deposit(10)

        assertEquals(20, result.balance)
    }

    @Test
    fun `eu deposito numa conta nova e o saldo fica igual ao valor depositado`() {
        val account = Account.openWith(id = AccountId("100"), initialDeposit = 10)

        assertEquals(10, account.balance)
        assertEquals(AccountId("100"), account.id)
    }

    @Test
    fun `eu saco e o saldo subtrai`() {
        val account = Account(id = AccountId("100"), balance = 20)

        val result = account.withdraw(5)

        assertEquals(15, result.balance)
    }

    @Test
    fun `deposit nao altera a conta original pois Account e imutavel`() {
        val account = Account(id = AccountId("100"), balance = 10)

        account.deposit(10)

        assertEquals(10, account.balance)
    }

    @Test
    fun `eu saco exatamente o saldo disponivel e a conta fica com zero`() {
        val account = Account(id = AccountId("100"), balance = 15)

        val result = account.withdraw(15)

        assertEquals(0, result.balance)
    }

    @Test
    fun `eu tento sacar mais do que o saldo disponivel e recebo InsufficientFundsException`() {
        val account = Account(id = AccountId("100"), balance = 10)

        val exception = assertFailsWith<InsufficientFundsException> {
            account.withdraw(11)
        }

        assertEquals(AccountId("100"), exception.accountId)
    }

    @Test
    fun `saque que falha por saldo insuficiente nao altera o saldo da conta original`() {
        val account = Account(id = AccountId("100"), balance = 10)

        runCatching { account.withdraw(11) }

        assertEquals(10, account.balance)
    }
}
