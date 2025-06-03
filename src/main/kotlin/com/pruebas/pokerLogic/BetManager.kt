package com.pruebas.pokerLogic


/**
 * clase que gestiona las apuestas
 */
class BetManager {

    // apuestas que se  han hecho esta ronda
    val madeBets = mutableListOf<Bet>()

    /**
     * funcion que resetea todo para una nueva ronda
     * @param players los jugadores que reinicia
     */
    fun resetBets(players: List<Player>) {
        players.forEach { it.currentBet = 0 }
        clear()
    }

    /**
     * funcion para hacer una apuesta y almacenarla
     */
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

    /**
     * funcion para tener la cantindad total de dinero en la mesa
     * @return la cantidad
     */
    fun totalAmount(): Int {
        return madeBets.sumOf{ it.amount }
    }

    /**
     * limpia las apuestas
     */
    fun clear() {
        madeBets.clear()
    }

}