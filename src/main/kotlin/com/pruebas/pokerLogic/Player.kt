package com.pruebas.pokerLogic

import org.springframework.web.socket.WebSocketSession

data class Player(
    val session: WebSocketSession,
    var name:String,
    var dinero:Int,
    var cards: MutableList<Card> = mutableListOf<Card>(),
    var hand: Hand? = null,
    var hasFolded:Boolean = false,
    var isSmallBlind:Boolean = false,
    var isBigBlind:Boolean = false,
    var isReady:Boolean = false,
){
    fun resetForNewHand() {
        cards.clear()
        hasFolded = false
    }

    fun giveCard(card: Card) {
        cards.add(card)
    }

    fun fold() {
        hasFolded = true
    }
}
