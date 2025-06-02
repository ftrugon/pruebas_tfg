package com.pruebas.pokerLogic

/**
 * clase para almacenar una apuesta en el betmanager
 */
data class Bet(
    val player: Player,
    val amount : Int
)