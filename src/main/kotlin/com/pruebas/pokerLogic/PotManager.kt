package com.pruebas.pokerLogic

class PotManager(private val betManager: BetManager) {

    fun calculateSidePots(): List<Pot> {
        // Apuestas totales por jugador (los retirados también aportan pero no ganan)
        val totalBets = betManager.madeBets
            .groupBy { it.player }
            .mapValues { (_, bets) -> bets.sumOf { it.amount } }
            .toMutableMap()

        val sidePots = mutableListOf<Pot>()

        while (totalBets.isNotEmpty()) {
            // Encontramos la menor apuesta entre los jugadores restantes
            val minBet = totalBets.values.minOrNull() ?: break

            // Jugadores que aún tienen al menos esa cantidad apostada
            val contributors = totalBets.filterValues { it >= minBet }.keys

            // Monto total del pot parcial (todos aportan esa cantidad)
            val potAmount = minBet * contributors.size

            // Solo los jugadores no retirados tienen derecho a ganar este pot
            val eligiblePlayers = contributors.filter {
                !it.hasFolded
            }

            sidePots.add(Pot(potAmount, eligiblePlayers))

            // Reducimos la apuesta de cada contribuidor
            contributors.forEach { player ->
                totalBets.computeIfPresent(player) { _, amount ->
                    val remaining = amount - minBet
                    if (remaining > 0) remaining else null
                }
            }
        }

        return sidePots
    }

}