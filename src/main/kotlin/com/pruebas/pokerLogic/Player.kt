package com.pruebas.pokerLogic

import org.springframework.web.socket.WebSocketSession

/**
 * clase de jugador, es un jugardor dentro de una mesa
 * @property session la sesion del websocket del jugadore
 * @property name el nobmre del jugador
 * @property tokens la cantidad de tokens que tiene el jugador
 */
data class Player(
    var session: WebSocketSession? = null,
    var name:String,
    var tokens:Int,
){
    var cards: MutableList<Card> = mutableListOf<Card>() // las 2 cartas que puede tener
    var hand: Hand? = null // la mano que puede tener
    var isSmallBlind:Boolean = false // por si es small blind
    var isBigBlind:Boolean = false // por si es big blind
    var isReadyToPlay:Boolean = false // por esta listo para jugar desde la lobby
    var hasFolded = false // indica si ha foldeado
    var playerState: PlayerState = PlayerState.NOT_READY // estado del jugador dentro de laa partida
    var currentBet: Int = 0 // apuesta actual que hace el jugador

    fun resetForNewHand() {
        isReadyToPlay = false
        cards.clear()
        hasFolded = false
        playerState = PlayerState.NOT_READY
    }

    fun giveCard(card: Card) {
        cards.add(card)
    }

    fun fold() {
        hasFolded = true
    }
}
