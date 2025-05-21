package com.pruebas

import com.pruebas.pokerLogic.BetManager
import com.pruebas.pokerLogic.Card
import com.pruebas.pokerLogic.Deck
import com.pruebas.pokerLogic.HandManager
import com.pruebas.pokerLogic.Player
import com.pruebas.pokerLogic.PlayerState
import com.pruebas.pokerLogic.PotManager
import com.pruebas.service.TableService
import com.pruebas.service.UsuarioService
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.web.socket.*
import org.springframework.web.socket.handler.TextWebSocketHandler


class PokerWebSocketHandler(
    private val tableId: String,
    private val bigBlindAmount: Int,
    private var saldoService: UsuarioService,
    private var tableService: TableService,
) : TextWebSocketHandler() {

    private val players = mutableListOf<Player>()
    private var playersToKick = mutableListOf<Player>()
    private var activePlayers = mutableListOf<Player>()
    private var handManager = HandManager()
    private var betManager = BetManager()
    private var potManager = PotManager(betManager)
    private var deck = Deck()
    private var communityCards = mutableListOf<Card>()
    private var gameActive = false
    private var dealerIndex = 0
    private var actualPlayerIndex = dealerIndex + 1

    private var smallBlindAmount = bigBlindAmount / 2
    //private var bigBlindAmount = 5

    private var actualTurn = TurnType.PRE_FLOP // enum class para cadda rondda

    override fun afterConnectionEstablished(session: WebSocketSession) {

        if (playersToKick.size < 6) {
            players.add(Player(session,"",0))
        }else {
            afterConnectionClosed(session, CloseStatus(4002,"No space on the table"))
        }
    }

    private fun assignRoles(){
        communityCards.clear()

        dealerIndex = (dealerIndex + 1) % players.size
        actualPlayerIndex = dealerIndex
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

    }

    private fun giveCards(){
        players.forEach {
            val card1 = deck.drawCard()
            val card2 = deck.drawCard()
            it.giveCard(card1)
            it.giveCard(card2)

            val jsonListOfCards = Json.encodeToString<MutableList<Card>>(it.cards)
            val messageToSend = Message(MessageType.PLAYER_CARDS,jsonListOfCards)
            val jsonMesagge = Json.encodeToString(messageToSend)
            it.session?.sendMessage(TextMessage(jsonMesagge))

        }
    }

    private fun betBlinds(){

        val smallBlindPlayer = activePlayers.find { it.isSmallBlind }
        if(smallBlindPlayer == null){
            throw NullPointerException("Player not found")
        }
        broadcast(Message(MessageType.TEXT_MESSAGE,"${smallBlindPlayer.name} was small blind, has bet $smallBlindAmount"))
        betManager.makeBet(smallBlindPlayer,smallBlindAmount)

        val bigBlindPlayer = activePlayers.find { it.isBigBlind }
        if(bigBlindPlayer == null){
            throw NullPointerException("Player not found")
        }
        broadcast(Message(MessageType.TEXT_MESSAGE,"${bigBlindPlayer.name} was big blind, has bet $bigBlindAmount"))
        betManager.makeBet(bigBlindPlayer,bigBlindAmount)


        actualPlayerIndex += 3

    }

    private fun startRound(){

        gameActive = true
        activePlayers = players.toMutableList()
        assignRoles()
        giveCards()
        betBlinds()

    }

    private fun addToCommunityCards(numCards: Int){
        for (i in 0..<numCards){
            communityCards.add(deck.drawCard())
        }

        val jsonListOfCards = Json.encodeToString<MutableList<Card>>(communityCards)

        broadcast(Message(MessageType.STATE_UPDATE,jsonListOfCards))
    }

    private fun calculateHands(){
        activePlayers.forEach {player ->
            player.hand = handManager.calculateHand((player.cards + communityCards).sortedByDescending { it.value.weight })

            sendMessageToPlayer(player,Message(MessageType.TEXT_MESSAGE,player.hand?.ranking.toString()))
        }
    }

    private fun allPlayersReady(): Boolean {
        players.forEach {
            if (!it.isReadyToPlay) return false
        }
        return true
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {

        val sessionPlayer = players.find { it.session == session }

        if (gameActive){
            sessionPlayer?.session = null

            if (sessionPlayer == players[actualPlayerIndex % players.size]){
                sessionPlayer.playerState = PlayerState.RETIRED
                activePlayers.remove(sessionPlayer)
            }
        }else {

            players.remove(sessionPlayer)


            try {
                saldoService.addTokensToUser(sessionPlayer?.name ?: "" ,sessionPlayer?.tokens ?: 0)
            }catch (e: Exception){

            }

            super.afterConnectionClosed(session, status)
        }

    }

    private fun getPlayerInfo(player: Player,msgPayload: String){
        val playerInfo = Json.decodeFromString<PlayerInfoMessage>(msgPayload)

        println(playerInfo)

        try{
            player.name = playerInfo.name
            player.tokens = playerInfo.dinero

            saldoService.retireTokensToUser(player.name,playerInfo.dinero)
            tableService.sumOneNumOfPlayerFromTable(tableId)

            broadcast(Message(MessageType.PLAYER_JOIN, player.name))

        }catch(e: Exception){
            afterConnectionClosed(player.session!!, CloseStatus(4001,e.message))
        }

    }

    private fun playerReady(player: Player){
        // Si la partida esta activa, no se puede dar a listo directamente
        if (!gameActive){
            player.isReadyToPlay = !player.isReadyToPlay
            broadcast(Message(MessageType.PLAYER_READY,player.name))

            // El poker requiere minimo de 3 personas para comenzar la partida, si no hay 3 personas en la lobby
            if (players.size >= 3 && allPlayersReady()) {
                broadcast(Message(MessageType.TEXT_MESSAGE, "The game is going to start!"))
                startRound()
            }
        }else{
            sendMessageToPlayer(player, Message(MessageType.TEXT_MESSAGE, "There is an active game right now"))
        }
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val action = message.payload

        val playerToChange = players.find { it.session == session }

        if (playerToChange == null) {
            throw NullPointerException("Player ${session.id} not found")
        }

        val msg = Json.decodeFromString<Message>(action)

        // PLAYER_INFO --> INFORMACION QUE LE LLEGA AL SERVIDOR DEL USUARIO
        if (msg.messageType == MessageType.PLAYER_INFO){
            getPlayerInfo(playerToChange,msg.content)
        }

        // PLAYER_READY --> Notificar al resto de los usuarios que un usuario esta listo
        if (msg.messageType == MessageType.PLAYER_READY){
            playerReady(playerToChange)
        }

        // TEXT_MESSAGE, --> Poner en un chat dentro del cliente el mensaje
        if (msg.messageType == MessageType.TEXT_MESSAGE){
            if (msg.content.isNotEmpty()){
                val msgToSend = Message(MessageType.TEXT_MESSAGE,"${playerToChange.name}: ${msg.content}")
                broadcast(msgToSend)
            }
        }

        // ACTION, --> Las distintas acciones que puede hace el usuario (fold, call, raise)
        if (msg.messageType == MessageType.ACTION){

            val betPayload = Json.decodeFromString<BetPayload>(msg.content)

            // compruebo si los jugadores estan listos, y el turno de cada jugador
            receiveBetAction(playerToChange,betPayload)

        }

    }

    private fun checkIfPassRound(): Boolean{
        players.forEach {


            if (it.session == null){
                it.playerState = PlayerState.RETIRED
                activePlayers.remove(it)
            }

            if (it.playerState == PlayerState.NOT_READY) return false
        }
        return true
    }

    private fun allPlayersToNotReady(){
        activePlayers.forEach {
            it.playerState = PlayerState.NOT_READY
        }
    }

    private fun chooseWinners(){

        val sidePots = potManager.calculateSidePots()

        for (i in sidePots.indices) {
            val winners = handManager.compareHands(sidePots[i].players)
            val prize = sidePots[i].amount / winners.size
            winners.forEach {
                broadcast(Message(MessageType.TEXT_MESSAGE,"${it.name} es un gandor del pot $i y se lleva $prize"))
                it.tokens += prize
            }
        }

    }

    private fun endRound(){


        players.forEach {
            it.isReadyToPlay = false
            it.cards.clear()
            it.playerState = PlayerState.NOT_READY
            it.currentBet = 0

            if (it.session == null){
                playersToKick.add(it)
            }
            if (it.tokens < bigBlindAmount){
                playersToKick.add(it)
            }

        }


        playersToKick.forEach {
            if (it.session != null){
                afterConnectionClosed(it.session!!, CloseStatus(4001,""))
            }
        }

        players.removeAll(playersToKick)


        actualTurn = TurnType.PRE_FLOP
        betManager.clear()
        gameActive = false
        broadcast(Message(MessageType.END_ROUND,""))

    }

    private fun newRound(){
        // dependiendo de si esta en el preflop, flop o river, sacara 3, 1 o no sacara cartas
        // por el momento paara probar solo voy a sacar 1 carta

        if (actualTurn == TurnType.SHOWDOWN || activePlayers.size == 1){

            // Catetada por si todo el mundo va all in de primeras
            if (actualTurn != TurnType.SHOWDOWN ){
                when (actualTurn){
                    TurnType.PRE_FLOP -> {
                        addToCommunityCards(5)
                    }
                    TurnType.FLOP -> {
                        addToCommunityCards(2)
                    }
                    TurnType.TURN -> {
                        addToCommunityCards(1)
                    }
                    else ->{
                        // nada jasjasj
                    }

                }
            }
            chooseWinners()
            endRound()
            return
        }

//        if (actualTurn == TurnType.RIVER){
//            actualTurn = TurnType.SHOWDOWN
//        }

        if (actualTurn == TurnType.TURN){
            actualTurn = TurnType.SHOWDOWN
            addToCommunityCards(1)
        }

        if (actualTurn == TurnType.FLOP){
            actualTurn = TurnType.TURN
            addToCommunityCards(1)
        }

        if (actualTurn == TurnType.PRE_FLOP){
            addToCommunityCards(3)
            actualTurn = TurnType.FLOP
        }


        allPlayersToNotReady()
        calculateHands()

    }

    private fun checkAllIn(player: Player){
        if (player.playerState == PlayerState.ALL_IN){
            activePlayers.remove(player)
        }
    }

    private fun receiveBetAction(player: Player, action: BetPayload){

        if (gameActive){


            if (player == activePlayers[actualPlayerIndex % activePlayers.size]){

                if (action.action == BetAction.RAISE && action.amount > 0){

                    val maxBet = activePlayers.maxOfOrNull { it.currentBet } ?: 0
                    val amountToCall = maxOf(0, maxBet - player.currentBet)
                    val amountToBet = amountToCall + action.amount

                    if (amountToBet <= player.tokens){
                        broadcast(Message(MessageType.TEXT_MESSAGE,"'${player.name}' raised $amountToBet"))

                        player.playerState = PlayerState.READY

                        betManager.makeBet(player,amountToBet)

                        allPlayersToNotReady()
                        checkAllIn(player)
                    }else{
                        sendMessageToPlayer(player,Message(MessageType.TEXT_MESSAGE,"You dont have enough tokens, all your tokens will be bet"))

                        betManager.makeBet(player,player.tokens)

                        allPlayersToNotReady()
                        checkAllIn(player)
                    }



                }else if (action.action == BetAction.CALL){

                    player.playerState = PlayerState.READY

                    val maxBet = activePlayers.maxOfOrNull { it.currentBet } ?: 0
                    val amountToCall = maxOf(0, maxBet - player.currentBet)

                    when {
                        amountToCall > player.tokens -> {
                            sendMessageToPlayer(player, Message(MessageType.TEXT_MESSAGE, "You don't have enough tokens, going all-in"))
                            betManager.makeBet(player, player.tokens)
                            checkAllIn(player)
                        }
                        amountToCall > 0 -> {
                            betManager.makeBet(player, amountToCall)
                            broadcast(Message(MessageType.TEXT_MESSAGE, "'${player.name}' called with $amountToCall"))
                        }
                        else -> {
                            broadcast(Message(MessageType.TEXT_MESSAGE, "'${player.name}' checked"))
                        }
                    }

                }else if (action.action == BetAction.FOLD){

                    activePlayers.remove(player)
                    player.playerState = PlayerState.RETIRED

                }

                if (checkIfPassRound()){
                    newRound()
                }

                if (player.tokens <= 0){
                    activePlayers.remove(player)
                }

                actualPlayerIndex++

            }else{
                sendMessageToPlayer(player,Message(MessageType.TEXT_MESSAGE,"Is not your turn"))
            }


        }else{
            sendMessageToPlayer(player,Message(MessageType.TEXT_MESSAGE,"You cant make bets right now"))
        }

    }

    private fun sendMessageToPlayer(player: Player, message: Message){
        val jsonMsg = Json.encodeToString(message)
        player.session?.sendMessage(TextMessage(jsonMsg))
    }

    private fun broadcast(message: Message) {
        val jsonMessage = Json.encodeToString<Message>(message)
        players.forEach { it.session?.sendMessage(TextMessage(jsonMessage)) }
    }

}
