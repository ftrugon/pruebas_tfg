package com.pruebas.pokerLogic

class BetManager {


    val makedBets = mutableListOf<Bet>()


    fun makeBet(jugador: Player, cantidad: Int) {
        makedBets.add(Bet(jugador, cantidad))
    }

    fun calculateTotalAmount(): Int {
        return makedBets.sumOf { it.cantity }
    }

    fun betsPerPlayer(jugador: Player): List<Bet> {
        return makedBets.filter { it.player == jugador }
    }

    fun totalAmountBetPerPlayer(jugador: Player): Int {
        return makedBets.filter { it.player == jugador }.sumOf { it.cantity }
    }

    fun clear() {
        makedBets.clear()
    }


}