package com.pruebas

import com.pruebas.pokerLogic.Card
import com.pruebas.pokerLogic.CardSuit
import com.pruebas.pokerLogic.CardValue
import com.pruebas.pokerLogic.Hand
import com.pruebas.pokerLogic.HandManager
import com.pruebas.pokerLogic.Player
import com.pruebas.pokerLogic.RankingPlay
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PokerLogicTest {


    val handManager = HandManager()

    // TESTS PARA PROBAR QUE EL HAND MANAGER EVALUA BIEN

    @Test
    fun `detects Royal Flush`() {
        val cards = listOf(
            Card(CardSuit.HEARTS, CardValue.AS),
            Card(CardSuit.HEARTS, CardValue.K),
            Card(CardSuit.HEARTS, CardValue.Q),
            Card(CardSuit.HEARTS, CardValue.J),
            Card(CardSuit.HEARTS, CardValue.TEN)
        )
        val result = handManager.calculateHand(cards)
        assert(result.ranking == RankingPlay.ROYAL_FLUSH)
    }

    @Test
    fun `detects Straight Flush`() {
        val cards = listOf(
            Card(CardSuit.CLUBS, CardValue.NINE),
            Card(CardSuit.CLUBS, CardValue.EIGHT),
            Card(CardSuit.CLUBS, CardValue.SEVEN),
            Card(CardSuit.CLUBS, CardValue.SIX),
            Card(CardSuit.CLUBS, CardValue.FIVE)
        )
        val result = handManager.calculateHand(cards)
        assert(result.ranking == RankingPlay.STRAIGHT_FLUSH)
    }

    @Test
    fun `detects Four of a Kind`() {
        val cards = listOf(
            Card(CardSuit.HEARTS, CardValue.TEN),
            Card(CardSuit.SPADES, CardValue.TEN),
            Card(CardSuit.CLUBS, CardValue.TEN),
            Card(CardSuit.DIAMONDS, CardValue.TEN),
            Card(CardSuit.HEARTS, CardValue.TWO)
        )
        val result = handManager.calculateHand(cards)
        assert(result.ranking == RankingPlay.FOUR_OF_A_KIND)
    }

    @Test
    fun `detects Full House`() {
        val cards = listOf(
            Card(CardSuit.HEARTS, CardValue.K),
            Card(CardSuit.SPADES, CardValue.K),
            Card(CardSuit.CLUBS, CardValue.K),
            Card(CardSuit.HEARTS, CardValue.TWO),
            Card(CardSuit.DIAMONDS, CardValue.TWO)
        )
        val result = handManager.calculateHand(cards)
        assert(result.ranking == RankingPlay.FULL_HOUSE)
    }

    @Test
    fun `detects Flush`() {
        val cards = listOf(
            Card(CardSuit.SPADES, CardValue.K),
            Card(CardSuit.SPADES, CardValue.J),
            Card(CardSuit.SPADES, CardValue.SEVEN),
            Card(CardSuit.SPADES, CardValue.FOUR),
            Card(CardSuit.SPADES, CardValue.TWO)
        )
        val result = handManager.calculateHand(cards)
        assert(result.ranking == RankingPlay.FLUSH)
    }

    @Test
    fun `detects Straight`() {
        val cards = listOf(
            Card(CardSuit.HEARTS, CardValue.EIGHT),
            Card(CardSuit.DIAMONDS, CardValue.SEVEN),
            Card(CardSuit.SPADES, CardValue.SIX),
            Card(CardSuit.CLUBS, CardValue.FIVE),
            Card(CardSuit.HEARTS, CardValue.FOUR)
        )
        val result = handManager.calculateHand(cards)
        assert(result.ranking == RankingPlay.STRAIGHT)
    }

    @Test
    fun `detects Three of a Kind`() {
        val cards = listOf(
            Card(CardSuit.HEARTS, CardValue.THREE),
            Card(CardSuit.SPADES, CardValue.THREE),
            Card(CardSuit.CLUBS, CardValue.THREE),
            Card(CardSuit.HEARTS, CardValue.TWO),
            Card(CardSuit.DIAMONDS, CardValue.FOUR)
        )
        val result = handManager.calculateHand(cards)
        assert(result.ranking == RankingPlay.THREE_OF_A_KIND)
    }

    @Test
    fun `detects Two Pair`() {
        val cards = listOf(
            Card(CardSuit.HEARTS, CardValue.EIGHT),
            Card(CardSuit.SPADES, CardValue.EIGHT),
            Card(CardSuit.CLUBS, CardValue.FIVE),
            Card(CardSuit.DIAMONDS, CardValue.FIVE),
            Card(CardSuit.HEARTS, CardValue.TWO)
        )
        val result = handManager.calculateHand(cards)
        assert(result.ranking == RankingPlay.TWO_PAIR)
    }

    @Test
    fun `detects One Pair`() {
        val cards = listOf(
            Card(CardSuit.HEARTS, CardValue.K),
            Card(CardSuit.SPADES, CardValue.K),
            Card(CardSuit.CLUBS, CardValue.THREE),
            Card(CardSuit.DIAMONDS, CardValue.FOUR),
            Card(CardSuit.HEARTS, CardValue.SIX)
        )
        val result = handManager.calculateHand(cards)
        assert(result.ranking == RankingPlay.ONE_PAIR)
    }

    @Test
    fun `detects High Card`() {
        val cards = listOf(
            Card(CardSuit.HEARTS, CardValue.AS),
            Card(CardSuit.SPADES, CardValue.TEN),
            Card(CardSuit.CLUBS, CardValue.SEVEN),
            Card(CardSuit.DIAMONDS, CardValue.FOUR),
            Card(CardSuit.HEARTS, CardValue.THREE)
        )
        val result = handManager.calculateHand(cards)
        assert(result.ranking == RankingPlay.HIGH_CARD)
    }

    // TEST PARA VER QUE COMPARA BIEN LAS CARTAS

    @Test
    fun `compareHands returns player with highest hand`() {

        val royalFlushCards = listOf(
            Card(CardSuit.HEARTS, CardValue.TEN),
            Card(CardSuit.HEARTS, CardValue.J),
            Card(CardSuit.HEARTS, CardValue.Q),
            Card(CardSuit.HEARTS, CardValue.K),
            Card(CardSuit.HEARTS, CardValue.AS)
        )

        val fullHouseCards = listOf(
            Card(CardSuit.CLUBS, CardValue.K),
            Card(CardSuit.DIAMONDS, CardValue.K),
            Card(CardSuit.SPADES, CardValue.K),
            Card(CardSuit.HEARTS, CardValue.THREE),
            Card(CardSuit.DIAMONDS, CardValue.THREE)
        )

        val flushCards = listOf(
            Card(CardSuit.DIAMONDS, CardValue.TWO),
            Card(CardSuit.DIAMONDS, CardValue.FIVE),
            Card(CardSuit.DIAMONDS, CardValue.SEVEN),
            Card(CardSuit.DIAMONDS, CardValue.NINE),
            Card(CardSuit.DIAMONDS, CardValue.J)
        )

        val royalFlush = Hand(RankingPlay.ROYAL_FLUSH, royalFlushCards, emptyList())
        val fullHouse = Hand(RankingPlay.FULL_HOUSE, fullHouseCards, emptyList())
        val flush = Hand(RankingPlay.FLUSH, flushCards, emptyList())

        val player1 = Player(null,"royal",100)
        player1.hand = royalFlush

        val player2 = Player(null,"flush",100)
        player2.hand = flush

        val player3 = Player(null,"fullhouse",100)
        player3.hand = fullHouse

        val winners = handManager.compareHands(listOf(player1, player2, player3))

        assertEquals(1, winners.size)
        assertEquals("royal", winners[0].name)
    }

}