package com.pruebas

import kotlinx.serialization.Serializable

@Serializable
data class PlayerInfoMessage(
    val name:String,
    val dinero:Int,
) {
}