package com.ebanx.ledger.http

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class EventType {
    @SerialName("deposit") DEPOSIT,
    @SerialName("withdraw") WITHDRAW,
    @SerialName("transfer") TRANSFER,
}

@Serializable
data class EventRequest(
    val type: EventType,
    val destination: String? = null,
    val origin: String? = null,
    val amount: Int,
)

@Serializable
data class AccountView(val id: String, val balance: Int)

@Serializable
data class EventResponse(val origin: AccountView? = null, val destination: AccountView? = null)
