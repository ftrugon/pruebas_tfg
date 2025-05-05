package com.pruebas.pokerLogic

class BetManager {


    val madeBets = mutableListOf<Bet>()

    fun makeBet(player: Player, amount: Int) {
        madeBets.add(Bet(player, amount))
    }

    fun totalAmount(): Int {
        return madeBets.sumOf{ it.cantity }
    }

    fun betsByPlayer(player: Player): List<Bet> {
        return madeBets.filter { it.player == player }
    }

    fun totalBetByPlayer(player: Player): Int {
        return madeBets.filter { it.player == player }.sumOf { it.cantity }
    }

    fun getCurrentBet(player: Player): Int {
        return totalBetByPlayer(player)
    }

    fun clear() {
        madeBets.clear()
    }

}