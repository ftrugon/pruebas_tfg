package com.pruebas.pokerLogic

class HandManager {


    fun calculateHand(sortedCards: List<Card>): Hand{

        when {

            isRoyalFlush(sortedCards) ->{ // escalera real
                val royalFlush = findStraight(sortedCards)

                return Hand(RankingPlay.ROYAL_FLUSH, royalFlush, emptyList())
            }
            isStraightFlush(sortedCards) ->{ // escalera de color
                val straightFlush = findStraight(sortedCards)

                return Hand(RankingPlay.STRAIGHT_FLUSH, straightFlush, emptyList())
            }
            groupCards(sortedCards,4) -> { // caso para que sea pokjer
                val fourOfAKind = sortedCards.groupBy { it.value }.values.first { it.size == 4 }
                val kickers = sortedCards.filter { it.value != fourOfAKind[0].value }.take(1)

                return Hand(RankingPlay.FOUR_OF_A_KIND,fourOfAKind,kickers)
            }
            groupCards(sortedCards,3) && groupCards(sortedCards,2) -> { // caso para que sea full house
                val pairOrTrio = sortedCards.groupBy { it.value }.values.first { it.size >= 2 }
                val secondPairOrTrio = sortedCards.filter { it.value != pairOrTrio[0].value }.groupBy { it.value }.values.first { it.size >= 2 }
                val fullHouse = pairOrTrio + secondPairOrTrio

                return Hand(RankingPlay.FULL_HOUSE,fullHouse, listOf())
            }
            isFlush(sortedCards) -> { // color
                val flush = sortedCards.groupBy { it.suit }.values.first { it.size >= 5 }.take(5)

                return Hand(RankingPlay.FLUSH,flush, listOf())
            }
            findStraight(sortedCards).isNotEmpty() ->{ // escalesra
                val straight = findStraight(sortedCards)

                return Hand(RankingPlay.STRAIGHT, straight, listOf())
            }
            groupCards(sortedCards,3) -> { // caso para que sea trio
                val trio = sortedCards.groupBy { it.value }.values.first { it.size == 3 }
                val kickers = sortedCards.filter { it.value != trio[0].value }.take(2)

                return Hand(RankingPlay.THREE_OF_A_KIND,trio,kickers)
            }
            isTwoPair(sortedCards) ->{ // caso de doble pareja
                val pairs = sortedCards.groupBy { it.value }.values.filter { it.size == 2 }.sortedByDescending { it[0].value.weight }.take(2).flatten()
                val kickers = sortedCards.filter { it.value != pairs[0].value && it.value != pairs[2].value }.take(1)

                return Hand(RankingPlay.TWO_PAIR,pairs,kickers)

            }
            groupCards(sortedCards,2) -> { // caso para que sea pareja
                val pair = sortedCards.groupBy { it.value }.values.first { it.size == 2 }
                val kickers = sortedCards.filter { it.value != pair[0].value }.take(3)

                return Hand(RankingPlay.ONE_PAIR,pair,kickers)
            }
            else -> { // carta alta
                val cartaAlta = sortedCards.first()
                val kickers = sortedCards.filter { it != cartaAlta }.take(4)

                return Hand(RankingPlay.HIGH_CARD, listOf(cartaAlta),kickers)
            }
        }
    }

    private fun isRoyalFlush(cards:List<Card>):Boolean{
        return isFlush(cards) && findStraight(cards).isNotEmpty() && cards.any{it.value == CardValue.AS}
    }

    private fun isStraightFlush(cards: List<Card>):Boolean{
        return isFlush(cards) && findStraight(cards).isNotEmpty()
    }

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

    private fun isFlush(cards: List<Card>):Boolean{
        return cards.groupBy { it.suit }.any { it.value.size >= 5 }
    }

    private fun isTwoPair(cards: List<Card>):Boolean{
        return cards.groupBy { it.value }.filter { it.value.size == 2 }.size >= 2
    }

    private fun groupCards(cards:List<Card>, numCards:Int):Boolean{
        return cards.groupBy { it.value }.any { it.value.size == numCards }
    }

    fun compareHands(players:List<Player>):List<Player>{
        val sortedPlayers = players.groupBy { it.hand!!.ranking }.minByOrNull { it.key }

        val listToReturn = mutableListOf(sortedPlayers!!.value[0])
        if (sortedPlayers.value.size == 1){
            return sortedPlayers.value
        }
        if (sortedPlayers.value.size >= 2){



            for (i in 0..sortedPlayers.value.size - 2){

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


    // -1 es mejor jugadorUno
    // 0 es empate
    // 1 es mejor jugadorDos
    fun bestOfTwoHands(playerOne:Player, playerTwo:Player):Int{

        val handPlayerOne = playerOne.hand!!
        val handPlayerTwo = playerTwo.hand!!

        val cardsPlayerOne = handPlayerOne.cardsPlayed
        val cardsPlayerTwo = handPlayerTwo.cardsPlayed

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