package com.ebanx.ledger.http

import kotlinx.serialization.Serializable

/** Corpo recebido em POST /event, tal como o enunciado define. */
@Serializable
data class EventRequest(
    val type: String,
    val destination: String? = null,
    val origin: String? = null,
    val amount: Int,
)

/** Representação de uma conta na resposta HTTP. */
@Serializable
data class AccountView(val id: String, val balance: Int)

/**
 * Corpo devolvido por POST /event. Os campos nulos são omitidos na
 * serialização (configurado no plugin de ContentNegotiation) porque o
 * enunciado só inclui a chave "origin" ou "destination" quando ela faz
 * sentido pro tipo de evento.
 */
@Serializable
data class EventResponse(val origin: AccountView? = null, val destination: AccountView? = null)
