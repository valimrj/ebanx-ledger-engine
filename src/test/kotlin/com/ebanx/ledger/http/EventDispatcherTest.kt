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
 * EventDispatcher is the one place that decides, from the `type` received
 * in the JSON, which use case to call. I isolate that decision here,
 * outside the Ktor route, to keep the HTTP handler's cyclomatic complexity
 * low — it only parses and delegates.
 */
class EventDispatcherTest {

    private val repository = InMemoryAccountRepository()
    private val dispatcher = EventDispatcher(
        depositUseCase = DepositUseCase(repository),
        withdrawUseCase = WithdrawUseCase(repository),
        transferUseCase = TransferUseCase(WithdrawUseCase(repository), DepositUseCase(repository)),
    )

    @Test
    fun `a deposit event returns only destination`() {
        val response = dispatcher.dispatch(EventRequest(type = EventType.DEPOSIT, destination = "100", amount = 10))

        assertEquals(EventResponse(destination = AccountView("100", 10)), response)
    }

    @Test
    fun `a withdraw event returns only origin`() {
        dispatcher.dispatch(EventRequest(type = EventType.DEPOSIT, destination = "100", amount = 20))

        val response = dispatcher.dispatch(EventRequest(type = EventType.WITHDRAW, origin = "100", amount = 5))

        assertEquals(EventResponse(origin = AccountView("100", 15)), response)
    }

    @Test
    fun `a withdraw event on a non-existing account propagates AccountNotFoundException`() {
        assertFailsWith<AccountNotFoundException> {
            dispatcher.dispatch(EventRequest(type = EventType.WITHDRAW, origin = "200", amount = 10))
        }
    }

    @Test
    fun `a transfer event returns origin and destination`() {
        dispatcher.dispatch(EventRequest(type = EventType.DEPOSIT, destination = "100", amount = 15))

        val response = dispatcher.dispatch(
            EventRequest(type = EventType.TRANSFER, origin = "100", destination = "300", amount = 15),
        )

        assertEquals(
            EventResponse(origin = AccountView("100", 0), destination = AccountView("300", 15)),
            response,
        )
    }

    /**
     * These cases are independent of each other — each one opens its own
     * in-memory repository from scratch — so `DynamicTest` fits well here:
     * the table below documents by itself which deposit amounts I cover,
     * and a new amount is just a new row.
     */
    @TestFactory
    fun `a deposit into a new account always opens it with a balance equal to the deposited amount`(): List<DynamicTest> {
        val amounts = listOf(1, 10, 100, 999)

        return amounts.map { amount ->
            DynamicTest.dynamicTest("depositing $amount opens the account with balance $amount") {
                val isolatedDispatcher = EventDispatcher(
                    depositUseCase = DepositUseCase(InMemoryAccountRepository()),
                    withdrawUseCase = WithdrawUseCase(InMemoryAccountRepository()),
                    transferUseCase = TransferUseCase(
                        WithdrawUseCase(InMemoryAccountRepository()),
                        DepositUseCase(InMemoryAccountRepository()),
                    ),
                )

                val response = isolatedDispatcher.dispatch(
                    EventRequest(type = EventType.DEPOSIT, destination = "100", amount = amount),
                )

                assertEquals(EventResponse(destination = AccountView("100", amount)), response)
            }
        }
    }
}
