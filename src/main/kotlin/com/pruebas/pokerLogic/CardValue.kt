package com.pruebas.pokerLogic

import kotlinx.serialization.Serializable

/**
 * enum para los valores de las cartas
 */
@Serializable
enum class CardValue(val weight: Int) {
    TWO(2), THREE(3), FOUR(4), FIVE(5), SIX(6), SEVEN(7), EIGHT(8), NINE(9), TEN(10),
    J(11), Q(12), K(13), AS(14)
}