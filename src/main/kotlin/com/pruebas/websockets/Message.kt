package com.pruebas.websockets

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val messageType: MessageType,
    val content: String,
) {
}