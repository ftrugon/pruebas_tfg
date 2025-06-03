package com.pruebas.pokerLogic

/**
 * clase que almacena un pot
 */
data class Pot(
    val amount: Int,
    val players: List<Player>,
) {
}