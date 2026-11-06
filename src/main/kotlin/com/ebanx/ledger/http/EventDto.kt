package com.ebanx.ledger.http

import kotlinx.serialization.Serializable

/** Body received on POST /event, exactly as the assignment defines it. */
@Serializable
data class EventRequest(
    val type: String,
    val destination: String? = null,
    val origin: String? = null,
    val amount: Int,
)

/** An account's representation in the HTTP response. */
@Serializable
data class AccountView(val id: String, val balance: Int)

/**
 * Body returned by POST /event. Null fields are omitted from
 * serialization (configured in the ContentNegotiation plugin) because the
 * assignment only includes the "origin" or "destination" key when it
 * makes sense for the event type.
 */
@Serializable
data class EventResponse(val origin: AccountView? = null, val destination: AccountView? = null)
