package com.pruebas.pokerLogic

import org.springframework.web.socket.WebSocketSession

data class Player(
    val session: WebSocketSession,
    var name:String,
    var dinero:Int,
    var cards: MutableList<Card> = mutableListOf<Card>(),
    var hand: Hand? = null,
    var playerState: PlayerState = PlayerState.NOT_READY, // reemplazar por PlayerState, notReady, ready, retired, banned
    var isSmallBlind:Boolean = false,
    var isBigBlind:Boolean = false,
    var isReadyToPlay:Boolean = false,
){
    fun resetForNewHand() {
        cards.clear()
        playerState = PlayerState.NOT_READY
    }

    fun giveCard(card: Card) {
        cards.add(card)
    }

    fun fold() {
        playerState = PlayerState.RETIRED
    }
}
