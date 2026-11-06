package com.ebanx.ledger.http

import com.ebanx.ledger.module
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Story 11 of my plan: I reproduce the exact sequence of requests and
 * responses the EBANX assignment describes, including each response's
 * body, not just the status code.
 *
 * I write this as ONE sequential test, not as a `@TestFactory` with
 * `DynamicTest`: each step of the assignment depends on the balance the
 * previous step left behind (e.g. "deposit into existing account" only
 * makes sense after "create account with initial balance"), and all the
 * steps need to run against the same live test server, on the same
 * client. `DynamicTest` assumes cases that are independent of each other
 * - forcing that here would mean recreating the application at every step
 * (losing the state). A sequential test is the right model for an
 * ordered regression suite like this one.
 *
 * The variable-data tests that actually benefit from `DynamicTest` are in
 * `EventDispatcherTest` (story 8), where each case is independent.
 */
class EventScenariosTest {

    @Test
    fun `I reproduce the EBANX assignment's full scenario sequence`() = testApplication {
        application { module() }

        // reset state before starting tests
        client.post("/reset").let {
            assertEquals(HttpStatusCode.OK, it.status)
        }

        // get balance for non-existing account
        client.get("/balance?account_id=1234").let {
            assertEquals(HttpStatusCode.NotFound, it.status)
            assertEquals("0", it.bodyAsText())
        }

        // create account with initial balance
        postEvent("""{"type":"deposit","destination":"100","amount":10}""").let {
            assertEquals(HttpStatusCode.Created, it.status)
            assertEquals("""{"destination":{"id":"100","balance":10}}""", it.bodyAsText())
        }

        // deposit into existing account
        postEvent("""{"type":"deposit","destination":"100","amount":10}""").let {
            assertEquals(HttpStatusCode.Created, it.status)
            assertEquals("""{"destination":{"id":"100","balance":20}}""", it.bodyAsText())
        }

        // get balance for existing account
        client.get("/balance?account_id=100").let {
            assertEquals(HttpStatusCode.OK, it.status)
            assertEquals("20", it.bodyAsText())
        }

        // withdraw from non-existing account
        postEvent("""{"type":"withdraw","origin":"200","amount":10}""").let {
            assertEquals(HttpStatusCode.NotFound, it.status)
            assertEquals("0", it.bodyAsText())
        }

        // withdraw from existing account
        postEvent("""{"type":"withdraw","origin":"100","amount":5}""").let {
            assertEquals(HttpStatusCode.Created, it.status)
            assertEquals("""{"origin":{"id":"100","balance":15}}""", it.bodyAsText())
        }

        // transfer from existing account
        postEvent("""{"type":"transfer","origin":"100","destination":"300","amount":15}""").let {
            assertEquals(HttpStatusCode.Created, it.status)
            assertEquals(
                """{"origin":{"id":"100","balance":0},"destination":{"id":"300","balance":15}}""",
                it.bodyAsText(),
            )
        }

        // transfer from non-existing account
        postEvent("""{"type":"transfer","origin":"200","destination":"300","amount":15}""").let {
            assertEquals(HttpStatusCode.NotFound, it.status)
            assertEquals("0", it.bodyAsText())
        }
    }

    /**
     * This scenario isn't in the assignment's examples, but the EBANX
     * checklist explicitly states that a withdraw "respecting insufficient
     * funds" is a conceptual point to validate. It's not a 404 (account
     * "300" exists); I chose 400 because it's the request that's malformed
     * given the current state, not a resource that doesn't exist.
     */
    @Test
    fun `I try to withdraw more than the account has and get 400`() = testApplication {
        application { module() }

        postEvent("""{"type":"deposit","destination":"400","amount":10}""")

        val response = postEvent("""{"type":"withdraw","origin":"400","amount":11}""")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("0", response.bodyAsText())

        val balanceAfter = client.get("/balance?account_id=400")
        assertEquals("10", balanceAfter.bodyAsText())
    }

    /**
     * EBANX checklist (item 10, "common bugs"): "invalid values not
     * handled" / "inconsistent state after operations". A negative
     * withdraw would pass unharmed through the sufficient-funds check and
     * behave like a deposit - I prove here, on the real route, that this
     * doesn't happen: the request is rejected with 400 and the balance
     * doesn't move at all.
     */
    @Test
    fun `I try to withdraw a negative amount and get 400 without the balance changing`() = testApplication {
        application { module() }

        postEvent("""{"type":"deposit","destination":"600","amount":20}""")

        val response = postEvent("""{"type":"withdraw","origin":"600","amount":-5}""")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("0", response.bodyAsText())

        val balanceAfter = client.get("/balance?account_id=600")
        assertEquals("20", balanceAfter.bodyAsText())
    }

    /**
     * EBANX checklist's conceptual point: "GET must not have a side
     * effect". I call GET /balance several times on purpose and confirm
     * the balance is the same every time - on the real route, not just on
     * the isolated use case (that one I already tested in
     * GetBalanceUseCaseTest).
     */
    @Test
    fun `GET balance repeatedly never changes the balance`() = testApplication {
        application { module() }

        postEvent("""{"type":"deposit","destination":"500","amount":30}""")

        repeat(5) {
            val response = client.get("/balance?account_id=500")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("30", response.bodyAsText())
        }
    }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.postEvent(body: String) =
        client.post("/event") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
}
