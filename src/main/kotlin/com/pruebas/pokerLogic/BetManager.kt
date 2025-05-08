package com.pruebas.pokerLogic

class BetManager {


    val madeBets = mutableListOf<Bet>()

    fun makeBet(player: Player, amount: Int) {
        if (amount <= 0) return

        val realAmount = minOf(player.tokens, amount)

        player.tokens -= realAmount
        player.currentBet += realAmount

        if (player.tokens <= 0){
            player.playerState = PlayerState.ALL_IN
        }

        madeBets.add(Bet(player, realAmount))
    }

    fun totalAmount(): Int {
        return madeBets.sumOf{ it.amount }
    }

    fun betsByPlayer(player: Player): List<Bet> {
        return madeBets.filter { it.player == player }
    }

    fun totalBetByPlayer(player: Player): Int {
        return madeBets.filter { it.player == player }.sumOf { it.amount }
    }

    fun getCurrentBet(player: Player): Int {
        return totalBetByPlayer(player)
    }

    fun clear() {
        madeBets.clear()
    }

}