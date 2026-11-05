package com.ebanx.ledger.domain

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
