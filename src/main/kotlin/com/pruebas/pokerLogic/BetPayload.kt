package com.pruebas.pokerLogic

import kotlinx.serialization.Serializable


@Serializable
data class BetPayload(
    val action: BetAction,
    val amount: Int
) {
}