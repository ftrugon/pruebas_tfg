package com.pruebas.pokerLogic

data class Hand(
    var ranking: RankingPlay,
    var cardsPlayed: List<Card>,
    var kickers: List<Card>
) {
}