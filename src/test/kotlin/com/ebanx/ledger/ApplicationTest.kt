package com.ebanx.ledger

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun `I boot the application and get 404 for a route that doesn't exist yet`() = testApplication {
        application { module() }

        val response = client.get("/route-that-does-not-exist")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
