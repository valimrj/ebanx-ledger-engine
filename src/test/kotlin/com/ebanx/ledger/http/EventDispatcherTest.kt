package com.ebanx.ledger.http

import com.ebanx.ledger.application.AccountNotFoundException
import com.ebanx.ledger.application.DepositUseCase
import com.ebanx.ledger.application.GetBalanceUseCase
import com.ebanx.ledger.application.ResetUseCase
import com.ebanx.ledger.application.TransferUseCase
import com.ebanx.ledger.application.WithdrawUseCase
import com.ebanx.ledger.infrastructure.InMemoryAccountRepository
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * EventDispatcher é o único lugar que decide, a partir do `type` recebido no
 * JSON, qual caso de uso chamar. Eu isolo essa decisão aqui, fora da rota
 * Ktor, pra manter o handler HTTP com complexidade ciclomática baixa — ele só
 * faz parsing e delega.
 */
class EventDispatcherTest {

    private val repository = InMemoryAccountRepository()
    private val dispatcher = EventDispatcher(
        depositUseCase = DepositUseCase(repository),
        withdrawUseCase = WithdrawUseCase(repository),
        transferUseCase = TransferUseCase(WithdrawUseCase(repository), DepositUseCase(repository)),
    )

    @Test
    fun `evento deposit retorna somente destination`() {
        val response = dispatcher.dispatch(EventRequest(type = "deposit", destination = "100", amount = 10))

        assertEquals(EventResponse(destination = AccountView("100", 10)), response)
    }

    @Test
    fun `evento withdraw retorna somente origin`() {
        dispatcher.dispatch(EventRequest(type = "deposit", destination = "100", amount = 20))

        val response = dispatcher.dispatch(EventRequest(type = "withdraw", origin = "100", amount = 5))

        assertEquals(EventResponse(origin = AccountView("100", 15)), response)
    }

    @Test
    fun `evento withdraw de conta inexistente propaga AccountNotFoundException`() {
        assertFailsWith<AccountNotFoundException> {
            dispatcher.dispatch(EventRequest(type = "withdraw", origin = "200", amount = 10))
        }
    }

    @Test
    fun `evento transfer retorna origin e destination`() {
        dispatcher.dispatch(EventRequest(type = "deposit", destination = "100", amount = 15))

        val response = dispatcher.dispatch(
            EventRequest(type = "transfer", origin = "100", destination = "300", amount = 15),
        )

        assertEquals(
            EventResponse(origin = AccountView("100", 0), destination = AccountView("300", 15)),
            response,
        )
    }

    /**
     * Estes casos são independentes entre si — cada um abre seu próprio
     * repositório em memória do zero — então `DynamicTest` se encaixa bem
     * aqui: a tabela abaixo documenta por si só quais valores de depósito eu
     * cubro, e um valor novo é só uma linha nova.
     */
    @TestFactory
    fun `deposito numa conta nova sempre abre com o saldo igual ao valor depositado`(): List<DynamicTest> {
        val amounts = listOf(1, 10, 100, 999)

        return amounts.map { amount ->
            DynamicTest.dynamicTest("depósito de $amount abre a conta com saldo $amount") {
                val isolatedDispatcher = EventDispatcher(
                    depositUseCase = DepositUseCase(InMemoryAccountRepository()),
                    withdrawUseCase = WithdrawUseCase(InMemoryAccountRepository()),
                    transferUseCase = TransferUseCase(
                        WithdrawUseCase(InMemoryAccountRepository()),
                        DepositUseCase(InMemoryAccountRepository()),
                    ),
                )

                val response = isolatedDispatcher.dispatch(
                    EventRequest(type = "deposit", destination = "100", amount = amount),
                )

                assertEquals(EventResponse(destination = AccountView("100", amount)), response)
            }
        }
    }
}
