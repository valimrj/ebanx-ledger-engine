package com.ebanx.ledger.http

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The three event kinds the assignment defines — a closed set, so an enum
 * fits better here than a bare `String` compared against magic literals.
 * `@SerialName` keeps the wire format (lowercase, as the assignment's
 * JSON examples use it) independent from the Kotlin naming convention
 * for enum entries.
 */
@Serializable
enum class EventType {
    @SerialName("deposit") DEPOSIT,
    @SerialName("withdraw") WITHDRAW,
    @SerialName("transfer") TRANSFER,
}

/**
 * Body received on POST /event, exactly as the assignment defines it.
 * An unknown `type` now fails right at deserialization, before
 * `EventDispatcher` is even called.
 */
@Serializable
data class EventRequest(
    val type: EventType,
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
