package com.pruebas.pokerLogic

class PotManager(private val betManager: BetManager) {

    fun calculateSidePots(): List<Pot> {
        // Apuestas totales por jugador (incluyendo los que se retiraron)
        val totalBets = betManager.madeBets
            .groupBy { it.player }
            .mapValues { (_, bets) -> bets.sumOf { it.amount } }
            .toMutableMap()

        val sidePots = mutableListOf<Pot>()

        while (totalBets.isNotEmpty()) {
            val minBet = totalBets.values.minOrNull() ?: break

            // Jugadores que aún tienen al menos minBet apostado
            val contributors = totalBets.filterValues { it >= minBet }.keys

            // El dinero lo ponen todos los que aún tienen dinero en juego,
            // pero solo los que no están retirados pueden ganar ese bote.
            val eligiblePlayers = contributors.filter { it.playerState != PlayerState.RETIRED }

            val potAmount = minBet * contributors.size
            sidePots.add(Pot(potAmount, eligiblePlayers))

            contributors.forEach { player ->
                totalBets.computeIfPresent(player) { _, amount ->
                    val newAmount = amount - minBet
                    if (newAmount <= 0) null else newAmount
                }
            }
        }

        return sidePots
    }

}