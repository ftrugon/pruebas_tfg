package com.pruebas.pokerLogic

enum class PaloCarta{
    Picas,
    Diamantes,
    Treboles,
    Corazones
}

enum class ValorCarta(val peso: Int) {
    DOS(2), TRES(3), CUATRO(4), CINCO(5), SEIS(6), SIETE(7), OCHO(8), NUEVE(9), DIEZ(10),
    JOTA(11), REINA(12), REY(13), AS(14)
}

data class Carta(var palo :PaloCarta, var valor :ValorCarta){
    override fun toString(): String {
        return "Carta: $valor de $palo"
    }
}

class Baraja() {
    val cartas = mutableListOf<Carta>()

    init {
        llenarBaraja()
        barajar()
    }

    fun llenarBaraja(){
        for (palo in PaloCarta.entries){
            for (valor in ValorCarta.entries){
                cartas.add(Carta(palo,valor))
            }
        }
    }

    fun sacarCarta():Carta{
        val cartaToReturn = cartas.first()
        cartas.removeFirst()
        return cartaToReturn
    }


    fun barajar(){
        cartas.shuffle()
    }

}