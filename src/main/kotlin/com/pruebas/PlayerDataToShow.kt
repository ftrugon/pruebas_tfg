package com.pruebas

import kotlinx.serialization.Serializable

@Serializable
data class PlayerDataToShow(
    val name: String,
    val tokenAmount: Int
) {
}