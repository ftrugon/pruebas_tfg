package com.pruebas.pokerLogic

class Deck {
    val cards = mutableListOf<Card>()


    fun fillDeck(){
        for (suit in CardSuit.entries){
            for (value in CardValue.entries){
                cards.add(Card(suit = suit,value = value))
            }
        }
    }

    fun drawCard(): Card{
        val cartaToReturn = cards.first()
        cards.removeFirst()
        return cartaToReturn
    }

    fun shuffle(){
        cards.shuffle()
    }

    fun clear(){
        cards.clear()
    }
}