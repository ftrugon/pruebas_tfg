package com.pruebas.pokerLogic

class PotManager(private val betManager: BetManager) {

    fun calculateSidePots(): List<Pot> {
        // Mapa de jugador -> cantidad total apostada
        val betsPerPlayer = betManager
            .madeBets
            .groupBy { it.player }
            .mapValues { it.value.sumOf { a -> a.amount } }
            .toMutableMap()

        val pots = mutableListOf<Pot>()


        while (betsPerPlayer.isNotEmpty()) {
            val min = betsPerPlayer.values.minOrNull() ?: break
            val players = betsPerPlayer.filterValues { it >= min }.keys.toList()
            val amount = min * players.size
            pots.add(Pot(amount, players))

            players.forEach {
                betsPerPlayer[it] = betsPerPlayer[it]!! - min
                if (betsPerPlayer[it] == 0) betsPerPlayer.remove(it)
            }

        }

        return pots
    }


}