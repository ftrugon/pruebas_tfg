package com.pruebas.pokerLogic

enum class JugadaRanking {
    ESCALERA_REAL, ESCALERA_COLOR, POKER, FULL_HOUSE, COLOR, ESCALERA, TRIO, DOBLE_PAREJA, PAREJA, CARTA_ALTA
}

class Poker(private val jugadores :List<Jugador>) {

    private val baraja = Baraja()
    private val cartasSobreMesa = mutableListOf<Carta>()
    private var bote = 0

    fun iniciarRonda() {
        println("\n--- Nueva Ronda ---")
        cartasSobreMesa.clear()
        baraja.llenarBaraja()
        baraja.barajar()
        jugadores.forEach {
            it.cartas.add(baraja.sacarCarta())
            it.cartas.add(baraja.sacarCarta())
        }
        distribuirCartasComunitarias()
        elegirGanador()
    }

    fun elegirGanador(){
        val jugadas = mutableMapOf<Jugador,Pair<JugadaRanking,Int>>()

        jugadores.forEach {
            jugadas[it] = calcularMano(it.cartas)
        }
        for (jugada in jugadas){
            println(jugada)
        }

        val jugadaGanadora = jugadas.minByOrNull { it.value.first }

        println("La mano ganadora es: $jugadaGanadora")

        for (jugada in jugadas){
            if (jugada.value == jugadaGanadora){
                println("El jugador ${jugada.key.nombre} es un ganador, con la mano ${jugada.key.cartas}")
            }
        }

    }


    fun calcularMano(jugadorCartas: List<Carta>): Pair<JugadaRanking,Int> {
        val cartas = (jugadorCartas + cartasSobreMesa).sortedBy { it.valor }

        // Lógica para determinar la mejor jugada
        return when {
            esEscaleraReal(cartas) -> JugadaRanking.ESCALERA_REAL to 12
            esEscaleraColor(cartas) -> JugadaRanking.ESCALERA_COLOR to 11
            esPoker(cartas) -> JugadaRanking.POKER to 10
            esFullHouse(cartas) -> JugadaRanking.FULL_HOUSE to 9
            esColor(cartas) -> JugadaRanking.COLOR to 8
            esEscalera(cartas) -> JugadaRanking.ESCALERA to 7
            esTrio(cartas) -> JugadaRanking.TRIO to 6
            esDoblePareja(cartas) -> JugadaRanking.DOBLE_PAREJA to 5
            esPareja(cartas) -> JugadaRanking.PAREJA to 4
            else -> JugadaRanking.CARTA_ALTA to 3
        }
    }

    // Funciones auxiliares para evaluar manos
    fun esEscaleraReal(cartas: List<Carta>): Boolean {
        return esEscaleraColor(cartas) && cartas.any { it.valor == ValorCarta.AS }
    }

    fun esEscaleraColor(cartas: List<Carta>): Boolean {
        return esEscalera(cartas) && esColor(cartas)
    }

    fun esPoker(cartas: List<Carta>): Boolean {
        return cartas.groupBy { it.valor }.any { it.value.size == 4 }
    }

    fun esFullHouse(cartas: List<Carta>): Boolean {
        val grupos = cartas.groupBy { it.valor }
        return grupos.any { it.value.size == 3 } && grupos.any { it.value.size == 2 }
    }

    fun esColor(cartas: List<Carta>): Boolean {
        return cartas.groupBy { it.palo }.any { it.value.size >= 5 }
    }

    fun esEscalera(cartas: List<Carta>): Boolean {
        val valoresOrdenados = cartas.map { it.valor.peso }.distinct().sorted()
        for (i in 0..valoresOrdenados.size - 5) {
            if ((0..4).all { j -> valoresOrdenados[i + j] == valoresOrdenados[i] + j }) {
                return true
            }
        }
        return false
    }

    fun esTrio(cartas: List<Carta>): Boolean {
        return cartas.groupBy { it.valor }.any { it.value.size == 3 }
    }

    fun esDoblePareja(cartas: List<Carta>): Boolean {
        return cartas.groupBy { it.valor }.filter { it.value.size == 2 }.size >= 2
    }

    fun esPareja(cartas: List<Carta>): Boolean {
        return cartas.groupBy { it.valor }.any { it.value.size == 2 }
    }

    private fun distribuirCartasComunitarias() {
        cartasSobreMesa.addAll(
            listOf(
                baraja.sacarCarta(),
                baraja.sacarCarta(),
                baraja.sacarCarta(),
                baraja.sacarCarta(),
                baraja.sacarCarta())
        )
        println("Cartas comunitarias: $cartasSobreMesa")
    }

    private fun jugadoresActivos() = jugadores.count { it.hasFolded }


}