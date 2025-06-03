package com.pruebas.pokerLogic


/**
 * clase para la baraja
 */
class Deck {
    val cards = mutableListOf<Card>()

    /**
     * funcion para rellenar la baraja
     */
    fun fillDeck(){
        for (suit in CardSuit.entries){
            for (value in CardValue.entries){
                cards.add(Card(suit = suit,value = value))
            }
        }
    }

    /**
     * funcion para pillar una carta, en este caso eligo la primera
     * @return la carta recogida
     */
    fun drawCard(): Card{
        val cartaToReturn = cards.first()
        cards.removeFirst()
        return cartaToReturn
    }

    /**
     * funcion para barajar la baraja
     */
    fun shuffle(){
        cards.shuffle()
    }

    /**
     * funcion que limpia la baraja
     */
    fun clear(){
        cards.clear()
    }
}