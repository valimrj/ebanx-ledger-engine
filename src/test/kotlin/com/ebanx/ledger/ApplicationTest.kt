package com.ebanx.ledger

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Teste de fio-terra (história 1): eu confirmo que o harness de teste de
 * integração do Ktor sobe a aplicação de ponta a ponta, antes de eu escrever
 * qualquer rota de negócio.
 */
class ApplicationTest {

    @Test
    fun `eu subo a aplicacao e recebo 404 para uma rota que ainda nao existe`() = testApplication {
        application { module() }

        val response = client.get("/rota-que-nao-existe")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
