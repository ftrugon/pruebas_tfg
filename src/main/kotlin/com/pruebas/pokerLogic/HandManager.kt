package com.pruebas.pokerLogic

/**
 * Clase que me sirve para evaluar las manos de los jugadores
 */
class HandManager {

    /**
     * funcion para calcular una mano de un jugador
     * @param sortedCards las cartas del jugador
     */
    fun calculateHand(sortedCards: List<Card>): Hand{

        when {

            isRoyalFlush(sortedCards) ->{ // escalera real

                // solo puede haber una escalera asi que esas son las 5 cartas que pillo
                val royalFlush = findStraight(sortedCards)
                return Hand(RankingPlay.ROYAL_FLUSH, royalFlush, emptyList())

            }
            isStraightFlush(sortedCards) ->{ // escalera de color

                // solo puede haber una escalera asi que esas son las 5 cartas que pillo
                val straightFlush = findStraight(sortedCards)
                return Hand(RankingPlay.STRAIGHT_FLUSH, straightFlush, emptyList())
            }
            groupCards(sortedCards,4) -> { // caso para que sea pokjer

                // pillo las 4 cartas que tengan el mismo valor
                val fourOfAKind = sortedCards.groupBy { it.value }.values.first { it.size == 4 }
                // la carta mas alta que no sea del poker
                val kickers = sortedCards.filter { it.value != fourOfAKind[0].value }.take(1)

                return Hand(RankingPlay.FOUR_OF_A_KIND,fourOfAKind,kickers)
            }
            groupCards(sortedCards,3) && groupCards(sortedCards,2) -> { // caso para que sea full house
                // primer par o trio
                val pairOrTrio = sortedCards.groupBy { it.value }.values.first { it.size >= 2 }
                // segundo par o trio
                val secondPairOrTrio = sortedCards.filter { it.value != pairOrTrio[0].value }.groupBy { it.value }.values.first { it.size >= 2 }
                val fullHouse = pairOrTrio + secondPairOrTrio

                return Hand(RankingPlay.FULL_HOUSE,fullHouse, listOf())
            }
            isFlush(sortedCards) -> { // color
                // todas las cartas que tengan el mismo color y
                val flush = sortedCards.groupBy { it.suit }.values.first { it.size >= 5 }.take(5)

                return Hand(RankingPlay.FLUSH,flush, listOf())
            }
            findStraight(sortedCards).isNotEmpty() ->{ // escalesra
                // encuentro una escalera
                val straight = findStraight(sortedCards)
                return Hand(RankingPlay.STRAIGHT, straight, listOf())
            }
            groupCards(sortedCards,3) -> { // caso para que sea trio
                // encuentro un trio y le paaso las 2 mejores otras cartas
                val trio = sortedCards.groupBy { it.value }.values.first { it.size == 3 }
                val kickers = sortedCards.filter { it.value != trio[0].value }.take(2)

                return Hand(RankingPlay.THREE_OF_A_KIND,trio,kickers)
            }
            isTwoPair(sortedCards) ->{ // caso de doble pareja
                // encuentro las 2 primeras parejas y la carta mas alta como kicker que no tenga el mismo valor que la primera o segunda pareja
                val pairs = sortedCards.groupBy { it.value }.values.filter { it.size == 2 }.sortedByDescending { it[0].value.weight }.take(2).flatten()
                val kickers = sortedCards.filter { it.value != pairs[0].value && it.value != pairs[2].value }.take(1)

                return Hand(RankingPlay.TWO_PAIR,pairs,kickers)

            }
            groupCards(sortedCards,2) -> { // caso para que sea pareja
                // busca 1 pareja y los 3 kickers mas altos
                val pair = sortedCards.groupBy { it.value }.values.first { it.size == 2 }
                val kickers = sortedCards.filter { it.value != pair[0].value }.take(3)

                return Hand(RankingPlay.ONE_PAIR,pair,kickers)
            }
            else -> { // carta alta
                // retorna la cartaa mas alta y las otras 4 mas altas
                val cartaAlta = sortedCards.first()
                val kickers = sortedCards.filter { it != cartaAlta }.take(4)

                return Hand(RankingPlay.HIGH_CARD, listOf(cartaAlta),kickers)
            }
        }
    }

    /**
     * funcion que comprueba si la mano es una escalera real
     * @param cards las cartas
     * @return si es escalera o no
     */
    private fun isRoyalFlush(cards:List<Card>):Boolean{
        return isFlush(cards) && findStraight(cards).isNotEmpty() && cards.any{it.value == CardValue.AS}
    }

    /**
     * funcion que comprueba si la mano es una escalera de color
     * @param cards las cartas
     * @return si es escalera o no
     */
    private fun isStraightFlush(cards: List<Card>):Boolean{
        return isFlush(cards) && findStraight(cards).isNotEmpty()
    }

    /**
     * busca una escalera en las 7 cartas
     * @param cards las cartas
     * @return la escalera enocntrada, si no encunetra una devuleve una lista vacia
     */
    private fun findStraight(cards: List<Card>):List<Card>{
        val distinctCards = cards.distinctBy { it.value }

        val consecutive = mutableListOf<Card>()

        for (i in 0 until distinctCards.size-1){

            if (distinctCards[i].value.weight == distinctCards[i+1].value.weight + 1){

                if (consecutive.isEmpty()){
                    consecutive.add(distinctCards[i])
                }

                consecutive.add(distinctCards[i+1])
                if (consecutive.size == 5) return consecutive

            }else{
                consecutive.clear()
            }

        }

        return emptyList()
    }

    /**
     * compruba si es flush
     * @param cards las caartas
     * @return si es o no color
     */
    private fun isFlush(cards: List<Card>):Boolean{
        return cards.groupBy { it.suit }.any { it.value.size >= 5 }
    }

    /**
     * comprueba si hay doble pareja
     * @param cards las cartas
     * @return si es o no doble pareja
     */
    private fun isTwoPair(cards: List<Card>):Boolean{
        return cards.groupBy { it.value }.filter { it.value.size == 2 }.size >= 2
    }

    /**
     * funcion que comprueba si se pueden agrupar un numero de cartas
     * @param cards las cartas
     * @param numCards el numero de cartas que se quiere agrupar
     * @return si se han agrupado o no las cartas
     */
    private fun groupCards(cards:List<Card>, numCards:Int):Boolean{
        return cards.groupBy { it.value }.any { it.value.size == numCards }
    }

    /**
     * funcion para comparar manos entre jugadores
     * @param players los jugadores a comparar
     * @return el ganador o ganadores de esa mano
     */
    fun compareHands(players:List<Player>):List<Player>{
        //agrupo por el rankign de la mano
        val sortedPlayers = players.groupBy { it.hand!!.ranking }.minByOrNull { it.key }

        // pillo la mano mas fuerte, si solo tiene un jugador, ese es el ganador
        val listToReturn = mutableListOf(sortedPlayers!!.value[0])
        if (sortedPlayers.value.size == 1){
            return sortedPlayers.value
        }
        if (sortedPlayers.value.size >= 2){


            for (i in 0..sortedPlayers.value.size - 2){

                // -1 es mejor jugadorUno
                // 0 es empate
                // 1 es mejor jugadorDos
                val isDraw = bestOfTwoHands(sortedPlayers.value[i],sortedPlayers.value[i+1])

                when (isDraw) {
                    -1 -> {
                        listToReturn[0] = sortedPlayers.value[i]
                    }
                    0 -> {
                        listToReturn.add(sortedPlayers.value[i+1])
                    }
                    1 -> {
                        listToReturn[0] = sortedPlayers.value[i+1]
                    }
                }

            }

            return listToReturn

        }
        listToReturn.clear()
        return listToReturn
    }


    /**
     * funcion que compara 2 manos
     * @param playerOne el primer jugador con la mano
     * @param playerTwo el segundo jugador
     */
    private fun bestOfTwoHands(playerOne:Player, playerTwo:Player):Int{

        // Si alguno de los 2 jugadores ha foldeado, se elige al otro como ganador sin importar su mano
        if (playerOne.hasFolded){
            return 1
        }else if(playerTwo.hasFolded){
            return -1
        }

        val handPlayerOne = playerOne.hand!!
        val handPlayerTwo = playerTwo.hand!!

        val cardsPlayerOne = handPlayerOne.cardsPlayed
        val cardsPlayerTwo = handPlayerTwo.cardsPlayed

        // se pilla cada carta jugada de cada jugador y se compara, si es la misma, continua, si no se elige un ganador
        for (i in cardsPlayerOne.indices){

            val cardPlayerOne = cardsPlayerOne[i]
            val cardPlayerTwo = cardsPlayerTwo[i]

            if (cardPlayerOne.value.weight != cardPlayerTwo.value.weight){
                if (cardPlayerOne.value.weight > cardPlayerTwo.value.weight){
                    return -1
                }else if (cardPlayerOne.value.weight < cardPlayerTwo.value.weight){
                    return 1
                }
            }
        }

        // si las cartas jguadas son iguales se pasa a los kickers, si son iguales tambien se retorna 0, es decir, que hay empate

        val kickersPlayerOne = handPlayerOne.kickers
        val kickersPlayerTwo = handPlayerTwo.kickers

        for (i in kickersPlayerOne.indices){
            val kickerJugUno = kickersPlayerOne[i]
            val kickerJugDos = kickersPlayerTwo[i]

            if (kickerJugUno.value.weight != kickerJugDos.value.weight){
                if (kickerJugUno.value.weight > kickerJugDos.value.weight){
                    return -1
                }else if (kickerJugUno.value.weight < kickerJugDos.value.weight){
                    return 1
                }
            }
        }

        // Si todas las cartas son iguales es empate
        return 0
    }

}