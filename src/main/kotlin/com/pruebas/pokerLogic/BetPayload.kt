package com.pruebas.pokerLogic

import kotlinx.serialization.Serializable

/**
 * clase que el usuario usa al hacer una apuesta, se lo manda al servidor
 */
@Serializable
data class BetPayload(
    val action: BetAction,
    val amount: Int
) {
}