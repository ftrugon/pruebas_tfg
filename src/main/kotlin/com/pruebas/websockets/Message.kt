package com.pruebas.websockets

import kotlinx.serialization.Serializable

/**
 * clase principal para la comuncacion con el cliente
 * @property messageType el tipo de mensaje
 * @property content es el payload del mensaje, nunca va a ser un string, sera un json que se decodeara de la libreria
 */
@Serializable
data class Message(
    val messageType: MessageType,
    val content: String,
) {
}