package com.pruebas.pokerLogic

/**
 * clase para guardar la mano de cada jugador
 */
data class Hand(
    var ranking: RankingPlay,
    var cardsPlayed: List<Card>,
    var kickers: List<Card>
) {
}