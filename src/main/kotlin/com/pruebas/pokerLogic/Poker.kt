package com.pruebas.pokerLogic

enum class JugadaRanking {
    ESCALERA_REAL, ESCALERA_COLOR, POKER, FULL_HOUSE, COLOR, ESCALERA, TRIO, DOBLE_PAREJA, PAREJA, CARTA_ALTA
}


data class ResultadoMano(
    val ranking: JugadaRanking,
    val cartasJugada: List<Carta>,
    val kickers: List<Carta>
)


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

        jugadores.forEach {
            println("${it.nombre} --> "+it.cartas)
        }

        val jugadas = jugadores.associateWith { jugador ->
            calcularMano(jugador.cartas)
        }

        jugadas.forEach {
            println("${it.key.nombre} --> ${it.value.ranking}")
        }

        val mejorMano = jugadas.minByOrNull { it.value.ranking } // solo si ResultadoMano implementa Comparable

        val ganadores = jugadas.filter { (_, mano) ->
            compararManos(mano, mejorMano!!.value) == 0
        }.keys

        println(ganadores)

    }


    fun calcularMano(jugadorCartas: List<Carta>): ResultadoMano {
        val cartas = (jugadorCartas + cartasSobreMesa).sortedByDescending { it.valor.peso }

        return when {
            esEscaleraReal(cartas) -> ResultadoMano(JugadaRanking.ESCALERA_REAL, cartas.take(5), emptyList())
            esEscaleraColor(cartas) -> ResultadoMano(JugadaRanking.ESCALERA_COLOR, cartas.take(5), emptyList())
            esPoker(cartas) -> {
                val grupo = cartas.groupBy { it.valor }.values.first { it.size == 4 }
                val kicker = cartas.filter { it.valor != grupo[0].valor }.maxByOrNull { it.valor.peso }!!
                ResultadoMano(JugadaRanking.POKER, grupo, listOf(kicker))
            }
            esFullHouse(cartas) -> {
                val grupos = cartas.groupBy { it.valor }
                val trio = grupos.values.first { it.size == 3 }
                val pareja = grupos.values.filter { it.size >= 2 && it[0].valor != trio[0].valor }.maxByOrNull { it[0].valor.peso }!!
                ResultadoMano(JugadaRanking.FULL_HOUSE, trio + pareja.take(2), emptyList())
            }
            esColor(cartas) -> {
                val color = cartas.groupBy { it.palo }.values.first { it.size >= 5 }.take(5)
                ResultadoMano(JugadaRanking.COLOR, color, emptyList())
            }
            esEscalera(cartas) -> {
                val escalera = encontrarEscalera(cartas)
                ResultadoMano(JugadaRanking.ESCALERA, escalera, emptyList())
            }
            esTrio(cartas) -> {
                val trio = cartas.groupBy { it.valor }.values.first { it.size == 3 }
                val kickers = cartas.filter { it.valor != trio[0].valor }.take(2)
                ResultadoMano(JugadaRanking.TRIO, trio, kickers)
            }
            esDoblePareja(cartas) -> {
                val parejas = cartas.groupBy { it.valor }.values.filter { it.size == 2 }.sortedByDescending { it[0].valor.peso }.take(2)
                val kickers = cartas.filter { it.valor != parejas[0][0].valor && it.valor != parejas[1][0].valor }.take(1)
                ResultadoMano(JugadaRanking.DOBLE_PAREJA, parejas.flatten(), kickers)
            }
            esPareja(cartas) -> {
                val pareja = cartas.groupBy { it.valor }.values.first { it.size == 2 }
                val kickers = cartas.filter { it.valor != pareja[0].valor }.take(3)
                ResultadoMano(JugadaRanking.PAREJA, pareja, kickers)
            }
            else -> ResultadoMano(JugadaRanking.CARTA_ALTA, listOf(cartas[0]), cartas.drop(1).take(4))
        }
    }

    fun compararManos(a: ResultadoMano, b: ResultadoMano): Int {
        val rankCompare = a.ranking.compareTo(b.ranking)
        if (rankCompare != 0) return rankCompare

        for (i in a.cartasJugada.indices) {
            val valA = a.cartasJugada.getOrNull(i)?.valor?.peso ?: 0
            val valB = b.cartasJugada.getOrNull(i)?.valor?.peso ?: 0
            if (valA != valB) return -valA.compareTo(valB)
        }

        for (i in a.kickers.indices) {
            val valA = a.kickers.getOrNull(i)?.valor?.peso ?: 0
            val valB = b.kickers.getOrNull(i)?.valor?.peso ?: 0
            if (valA != valB) return -valA.compareTo(valB)
        }

        return 0
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
        return encontrarEscalera(cartas).isNotEmpty()
    }

    fun encontrarEscalera(cartas: List<Carta>): List<Carta> {
        val valoresUnicos = cartas.distinctBy { it.valor.peso }.sortedByDescending { it.valor.peso }
        for (i in 0..valoresUnicos.size - 5) {
            val sublista = valoresUnicos.subList(i, i + 5)
            val esSecuencia = sublista.zipWithNext().all { (a, b) -> a.valor.peso == b.valor.peso + 1 }
            if (esSecuencia) return sublista
        }
        return emptyList()
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