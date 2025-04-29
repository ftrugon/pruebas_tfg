package com.pruebas

import com.pruebas.pokerLogic.BetManager
import com.pruebas.pokerLogic.Card
import com.pruebas.pokerLogic.Deck
import com.pruebas.pokerLogic.HandManager
import com.pruebas.pokerLogic.Player
import com.pruebas.pokerLogic.PotManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.web.socket.*
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.Executors


class PokerWebSocketHandler(private val gameId: String) : TextWebSocketHandler() {

    private val players = mutableListOf<Player>()
    private val activePlayers = players

    private var currentPlayerIndex = 0
    private var handManager = HandManager()
    private var betManager = BetManager()
    private var potManager = PotManager(betManager)
    private var deck = Deck()
    private var communityCards = mutableListOf<Card>()

    private var gameActive = false
    private var dealerIndex = 0

    override fun afterConnectionEstablished(session: WebSocketSession) {

        players.add(Player(session,"",0))

    }

    private fun startRound(){

        communityCards.clear()

        dealerIndex = (dealerIndex + 1) % players.size
        val smallBlindIndex = (dealerIndex + 1) % players.size
        val bigBlindIndex = (dealerIndex + 2) % players.size

        players.forEach {
            it.isSmallBlind = false
            it.isBigBlind = false
        }

        players[smallBlindIndex].isSmallBlind = true
        players[bigBlindIndex].isBigBlind = true

        deck.clear()
        deck.fillDeck()
        deck.shuffle()

        giveCards()

        preFlop()
        flop()
        turn()
        river()
        showdown()

    }

    private fun endRound(){
        players.forEach { it.isReady = false }
    }

    private fun betRound(){
        if (activePlayers.isEmpty()) return

        var lastBet = 0
        var index = 0
        var numPlayersWhoCalled = 0


        while (numPlayersWhoCalled < activePlayers.size){
            val player = activePlayers[index % activePlayers.size]


            if (player.dinero <= 0) {
                numPlayersWhoCalled++
                index++
                continue
            }

            println("Turno de ${player.name} (dinero: ${player.dinero})")
            println("Tus cartas")
            println()
            for (carta in player.cards) {
                print("$carta, ")
            }
            println(" ---> Tu mano actual tiene ${player.hand?.ranking}")
            println("1. Foldear")
            println("2. Apostar / Igualar (mínimo para igualar: $lastBet)")

            // EN LUGAR de readdln aqui tiene quee ir algo para esperar un tiempo
            // mas que esperar, una instruccion que se ejecute cuanmdo el usuario finalmente le envie una señal al servidor del tipo correspondiente
            // y si no la envia en 30 secs foldea automatico
            // crear otra clase para la accion que se va a realizar

            // betPayload
            // betType --> otra data class
            // amount --> realmente esto solo te sirve si la accion es un raise
            // me da pereza hacer la clase y buscar informacion ahora mismo, por eso la razon de comentar esto
            val opcion = readln().toIntOrNull()




        }


        broadcast(Message(MessageType.TEXT_MESSAGE,"RONDA DE APUESTAS"))
    }

    private fun addToCommunityCards(numCards: Int){
        for (i in 0..<numCards){
            communityCards.add(deck.drawCard())
        }
        broadcast(Message(MessageType.STATE_UPDATE,"Community Cards: $communityCards"))
    }

    private fun calculateHands(){
        players.forEach {player ->
            player.hand = handManager.calculteHand((player.cards + communityCards).sortedByDescending { it.value.weight })
            val mssg = Message(MessageType.TEXT_MESSAGE,player.hand?.ranking.toString())
            val jsonmsg = Json.encodeToString(mssg)
            player.session.sendMessage(TextMessage(jsonmsg))
        }
    }

    fun preFlop(){
        betRound()
    }

    fun flop(){
        addToCommunityCards(3)
        calculateHands()
        betRound()
    }

    fun turn(){
        addToCommunityCards(1)
        calculateHands()
        betRound()
    }

    fun river(){
        addToCommunityCards(1)
        calculateHands()
        betRound()
    }

    fun showdown(){

        // repartir premios -->
        endRound()
    }


    fun giveCards(){
        players.forEach {
            val card1 = deck.drawCard()
            val card2 = deck.drawCard()
            it.giveCard(card1)
            it.giveCard(card2)

            val messageToSend = Message(MessageType.PLAYER_CARDS,listOf(card1,card2).toString())

            val jsonMesagge = Json.encodeToString(messageToSend)

            it.session.sendMessage(TextMessage(jsonMesagge))

        }
    }


    private fun allPlayersReady(): Boolean {
        players.forEach {
            if (!it.isReady) return false
        }
        return true
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        println("Conexion cerrada: ${session.id}")
        players.removeIf { it.session == session }
        super.afterConnectionClosed(session, status)

    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val action = message.payload

        val playerToChange = players.find { it.session == session }

        val message = Json.decodeFromString<Message>(action)

        if (message.messageType == MessageType.PLAYER_INFO){
            val playerInfo = Json.decodeFromString<PlayerInfoMessage>(message.content)

            playerToChange?.name = playerInfo.name
            playerToChange?.dinero = playerInfo.dinero

            broadcast(Message(MessageType.PLAYER_JOIN, "${playerToChange?.name}"))

            println(playerToChange)

        }


        // TODO LIST --> EL RESTO DE CASO DEL TIPO DE LOS MENSAJES,

        // PLAYER_READY --> Notificar al resto de los usuarios que un usuario esta listo

        if (message.messageType == MessageType.PLAYER_READY){

            if (!gameActive){
                playerToChange?.isReady = !playerToChange!!.isReady
                broadcast(Message(MessageType.PLAYER_READY,playerToChange!!.name))

                if (players.size >= 3 && allPlayersReady()) {
                    // METODO PARA EMPEZAR LA PARTIDA O LO QUE SEA
                    startRound()
                    broadcast(Message(MessageType.TEXT_MESSAGE, "LA PARTIDA VA A EMPEZAR"))
                }
            }

        }

        // TEXT_MESSAGE, --> Poner en un chat dentro del cliente el mensaje


        if (message.messageType == MessageType.TEXT_MESSAGE){
            broadcast(message)
        }


        // ESTOS 2 PUNTOS SON LOS COMPLICADOS, DE MOMENTO COMINUCACION CON EN CLIETNE

        // ACTION, --> Las distintas acciones que puede hace el usuario (fold, call, raise)

        println("Acción recibida: $action")

    }


    private fun broadcast(message: Message) {
        val jsonMessage = Json.encodeToString<Message>(message)
        players.forEach { it.session.sendMessage(TextMessage(jsonMessage)) }
    }

}
