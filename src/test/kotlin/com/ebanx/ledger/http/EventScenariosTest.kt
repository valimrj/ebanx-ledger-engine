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
 * História 11 do meu plano: reproduzo a sequência exata de requisições e
 * respostas que o enunciado da EBANX descreve, incluindo o corpo de cada
 * resposta, não só o status code.
 *
 * Eu escrevo isso como UM teste sequencial, não como `@TestFactory` com
 * `DynamicTest`: cada passo do enunciado depende do saldo que o passo
 * anterior deixou (ex.: "deposit into existing account" só faz sentido
 * depois de "create account with initial balance"), e todos os passos
 * precisam rodar contra o mesmo servidor de teste vivo, no mesmo client.
 * `DynamicTest` assume casos independentes entre si — forçar isso aqui ia
 * exigir recriar a aplicação a cada passo (perdendo o estado). Um teste
 * sequencial é a modelagem correta pra uma suíte de regressão ordenada
 * como esta.
 *
 * Os testes de dado variável que realmente se beneficiam de `DynamicTest`
 * eu escrevi em `EventDispatcherTest` (história 8), onde cada caso é
 * independente.
 */
class EventScenariosTest {

    @Test
    fun `eu reproduzo a sequencia completa de cenarios do enunciado da EBANX`() = testApplication {
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
     * Esse cenário não está nos exemplos do enunciado, mas o checklist da
     * EBANX deixa explícito que saque "respeitando saldo insuficiente" é um
     * ponto conceitual a validar. Não é 404 (a conta "300" existe); eu
     * escolhi 400 porque é o pedido que está malformado frente ao estado
     * atual, não um recurso que não existe.
     */
    @Test
    fun `eu tento sacar mais do que a conta tem e recebo 400`() = testApplication {
        application { module() }

        postEvent("""{"type":"deposit","destination":"400","amount":10}""")

        val response = postEvent("""{"type":"withdraw","origin":"400","amount":11}""")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("0", response.bodyAsText())

        val balanceAfter = client.get("/balance?account_id=400")
        assertEquals("10", balanceAfter.bodyAsText())
    }

    /**
     * Ponto conceitual do checklist da EBANX: "GET não deve ter efeito
     * colateral". Eu chamo GET /balance várias vezes de propósito e
     * confirmo que o saldo é o mesmo em todas — na rota real, não só no
     * caso de uso isolado (esse eu já testei em GetBalanceUseCaseTest).
     */
    @Test
    fun `GET balance repetidas vezes nunca muda o saldo`() = testApplication {
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
