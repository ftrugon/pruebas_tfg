package com.pruebas.pokerLogic


fun main() {

    val jugadores = listOf(Jugador("A",123),Jugador("B",123),Jugador("C",123))

    val poker = Poker(jugadores)


    poker.iniciarRonda()
}