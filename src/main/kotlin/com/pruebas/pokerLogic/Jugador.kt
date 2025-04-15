package com.pruebas.pokerLogic

data class Jugador(val nombre:String, var dinero:Int, var cartas: MutableList<Carta> = mutableListOf<Carta>(), var hasFolded:Boolean = false, val isSmall:Boolean = false, val isBig:Boolean = false)
