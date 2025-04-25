package com.pruebas.pokerLogic

data class Player(
    val name:String,
    var dinero:Int,
    var cards: MutableList<Card> = mutableListOf<Card>(),
    var hand: Hand? = null,
    var hasFolded:Boolean = false,
    var isSmall:Boolean = false,
    var isBig:Boolean = false,
    var isReady:Boolean = false,
)
