package com.pruebas

import com.pruebas.model.BetDocument
import com.pruebas.pokerLogic.BetManager
import com.pruebas.pokerLogic.Card
import com.pruebas.pokerLogic.Deck
import com.pruebas.pokerLogic.HandManager
import com.pruebas.pokerLogic.Player
import com.pruebas.pokerLogic.PlayerState
import com.pruebas.pokerLogic.PotManager
import com.pruebas.service.BetService
import com.pruebas.service.TableService
import com.pruebas.service.UsuarioService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.web.socket.*
import org.springframework.web.socket.handler.TextWebSocketHandler


class PokerWebSocketHandler(
    private val tableId: String,
    private val bigBlindAmount: Int,
    private var saldoService: UsuarioService,
    private var tableService: TableService,
    private var betService: BetService
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

    // funcion del websocket de cuando se establece una conexion
    override fun afterConnectionEstablished(session: WebSocketSession) {

        if (playersToKick.size < 6) {
            players.add(Player(session,"",0))
        }else {
            afterConnectionClosed(session, CloseStatus(4002,"No space on the table"))
        }
    }

    // funcion que se ejecuta al principio de cada ronda, para probarlo todo
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

    // reparte las cartas a cada usuario
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

    // funcion que se ejecuta al principio de cada partida, reparte los blinds y los turnos
    private fun betBlinds(){

        val smallBlindPlayer = activePlayers.find { it.isSmallBlind }
        if(smallBlindPlayer == null){
            throw NullPointerException("Player not found")
        }
        broadcast(Message(MessageType.SERVER_RESPONSE,"${smallBlindPlayer.name} was small blind, has bet $smallBlindAmount"))
        betManager.makeBet(smallBlindPlayer,smallBlindAmount)
        makeBetToService(smallBlindPlayer.name,smallBlindAmount,"Small blind")

        val bigBlindPlayer = activePlayers.find { it.isBigBlind }
        if(bigBlindPlayer == null){
            throw NullPointerException("Player not found")
        }
        broadcast(Message(MessageType.SERVER_RESPONSE,"${bigBlindPlayer.name} was big blind, has bet $bigBlindAmount"))
        betManager.makeBet(bigBlindPlayer,bigBlindAmount)
        makeBetToService(bigBlindPlayer.name,bigBlindAmount,"Big blind")

        actualPlayerIndex += 3
        val player = activePlayers[actualPlayerIndex % activePlayers.size]

        broadcast(Message(MessageType.NOTIFY_TURN, "${player.name}:${player.tokens}"))
    }

    private fun sendInfoOfPlayers(){
        val listToSend = mutableListOf<PlayerDataToShow>()

        activePlayers.forEach {
            listToSend.add(PlayerDataToShow(it.name,it.tokens))
        }

        val msgJson = Json.encodeToString<List<PlayerDataToShow>>(listToSend)
        broadcast(Message(MessageType.SEND_PLAYER_DATA,msgJson))
    }

    // funcion para empezar una ronda
    private fun startRound(){

        gameActive = true
        activePlayers = players.toMutableList()
        assignRoles()
        giveCards()

        sendInfoOfPlayers()

        broadcast(Message(MessageType.START_ROUND,""))
        betBlinds()

    }

    // reparte cartas a las cartas cominutarias y les manda a los usuairos cuales son
    private fun addToCommunityCards(numCards: Int){
        for (i in 0..<numCards){
            communityCards.add(deck.drawCard())
        }

        val jsonListOfCards = Json.encodeToString<MutableList<Card>>(communityCards)

        broadcast(Message(MessageType.STATE_UPDATE,jsonListOfCards))
    }

    // calcula las manos de cada jugador
    private fun calculateHands(){
        activePlayers.forEach {player ->
            player.hand = handManager.calculateHand((player.cards + communityCards).sortedByDescending { it.value.weight })

            sendMessageToPlayer(player,Message(MessageType.HAND_RANKING,player.hand?.ranking.toString()))
        }
    }

    // comprueba si todos los jugadores estan listos para jugar
    private fun allPlayersReady(): Boolean {
        players.forEach {
            if (!it.isReadyToPlay) return false
        }
        return true
    }

    // funcion del websocket de cuando se cierra una conexion
    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {

        val sessionPlayer = players.find { it.session == session }
        println("Se va a cerrar la conexion del jugador ${sessionPlayer?.name}")

        if (gameActive && sessionPlayer !in activePlayers) {
            sessionPlayer?.session = null

            //if (sessionPlayer == players[actualPlayerIndex % players.size]){
            sessionPlayer?.hasFolded = true
                //activePlayers.remove(sessionPlayer)
            //}

            if (sessionPlayer == players[actualPlayerIndex % players.size]){
                nextPlayerIndex()
            }
        }else {

            players.remove(sessionPlayer)

            try {
                saldoService.addTokensToUser(sessionPlayer?.name ?: "" ,sessionPlayer?.tokens ?: 0)
                tableService.subOneNumOfPlayerFromTable(tableId)
            }catch (e: Exception){
                println("Hubo una excepcion al persistir datos del usuario que se ha cerrado")
            }

            broadcast(Message(MessageType.PLAYER_LEAVE,sessionPlayer?.name ?: ""))

            super.afterConnectionClosed(session, status)
        }

    }

    // funcion para cuando un jugador se une a la mesa, el usuario manda automaticamente este mensaje
    private fun getPlayerInfo(player: Player,msgPayload: String){
        val playerInfo = Json.decodeFromString<PlayerInfoMessage>(msgPayload)

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

    // comprueba si un jugador esta listo y si las condiciones se dan comienza la partida
    private fun playerReady(player: Player){
        // Si la partida esta activa, no se puede dar a listo directamente
        if (!gameActive){
            player.isReadyToPlay = !player.isReadyToPlay
            val jsonToBool = Json.encodeToString<Pair<String, Boolean>>(player.name to player.isReadyToPlay)

            broadcast(Message(MessageType.PLAYER_READY,jsonToBool))

            // El poker requiere minimo de 3 personas para comenzar la partida, si no hay 3 personas en la lobby
            if (players.size >= 2 && allPlayersReady()) {
                broadcast(Message(MessageType.SERVER_RESPONSE, "The game is going to start!"))
                startRound()
            }
        }else{
            sendMessageToPlayer(player, Message(MessageType.SERVER_RESPONSE, "There is an active game right now"))
        }
    }

    // funcion para recibir un mensaje, lo convierto a json para parsear el mensaje con una clase personalizada y dependiendo de este hago una cosa u otra
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

    // comprueba si tiene que ir a la siguiente ronda
    private fun checkIfPassRound(): Boolean{
        return activePlayers
            .filter { it.playerState != PlayerState.ALL_IN && !it.hasFolded }
            .all { it.playerState == PlayerState.READY }
    }

    // pone todos los jugadores a not ready, por si alguien hace raise
    private fun allPlayersToNotReady(){
        activePlayers.forEach {
            if (it.playerState != PlayerState.ALL_IN && !it.hasFolded) {
                it.playerState = PlayerState.NOT_READY
            }
        }
    }

    // comprueba los ganadroes de la ronda actual
    private fun chooseWinners(){

        val sidePots = potManager.calculateSidePots()


        for (i in sidePots.indices) {
            val winners = handManager.compareHands(sidePots[i].players)
            val prize = sidePots[i].amount / winners.size
            winners.forEach {
                broadcast(Message(MessageType.NOTIFY_WINNER,"${it.name} es un gandor del pot ${i+1} y se lleva $prize"))
                it.tokens += prize
            }
        }

    }

    // funcion para acabar la ronda
    private fun endRound(){

        players.forEach {
            it.isReadyToPlay = false
            it.cards.clear()
            it.hasFolded = false
            it.playerState = PlayerState.NOT_READY

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
        betManager.resetBets(players)
        gameActive = false
        broadcast(Message(MessageType.END_ROUND,""))

    }

    // nuevo turno despues del preflop, flop ....
    private fun newTurn(){
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

    // funcion para calcular el proximo index de los jugadores
    private fun nextPlayerIndex() {
        if (activePlayers.isEmpty()){
            actualPlayerIndex = 0
            return
        }

        var index = (actualPlayerIndex + 1) % activePlayers.size

        while (activePlayers[index].hasFolded) {
            index = (index + 1) % activePlayers.size

            // Si hemos dado la vuelta completa y no hay nadie disponible
            if (index == actualPlayerIndex) break
        }

        actualPlayerIndex = index
        val player = activePlayers[actualPlayerIndex % activePlayers.size]
        broadcast(Message(MessageType.NOTIFY_TURN, "${player.name}:${player.tokens}"))
        sendInfoOfPlayers()
    }

    // Envia una apuesta a la base de datos
    fun makeBetToService(playerName: String, amount: Int, betType: String){
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = saldoService.getByUsername(playerName)._id
                betService.insertBet(BetDocument(null,tableId,userId?: "",amount,betType))
            }catch (e:Exception){
                println("Error while uploading bet on db")
            }
        }
    }

    // funcion para recibir la accion que ha hecho el jugador
    private fun receiveBetAction(player: Player, action: BetPayload){

        if (gameActive){

            if (player == activePlayers[actualPlayerIndex % activePlayers.size]){

                if (action.action == BetAction.RAISE && action.amount > 0){

                    val maxBet = activePlayers.maxOfOrNull { it.currentBet } ?: 0
                    val amountToCall = maxOf(0, maxBet - player.currentBet)
                    val amountToBet = amountToCall + action.amount

                    if (amountToBet <= player.tokens){
                        broadcast(Message(MessageType.SERVER_RESPONSE,"'${player.name}' raised $amountToBet"))

                        player.playerState = PlayerState.READY

                        betManager.makeBet(player,amountToBet)
                        makeBetToService(player.name, amountToBet,"Raise")

                        allPlayersToNotReady()

                    }else{
                        sendMessageToPlayer(player,Message(MessageType.SERVER_RESPONSE,"You don't have enough tokens, going all-in"))

                        betManager.makeBet(player,player.tokens)
                        makeBetToService(player.name, amountToBet,"All in")

                        allPlayersToNotReady()

                    }


                }else if (action.action == BetAction.CALL){

                    player.playerState = PlayerState.READY

                    val maxBet = activePlayers.maxOfOrNull { it.currentBet } ?: 0
                    val amountToCall = maxOf(0, maxBet - player.currentBet)

                    when {
                        amountToCall > player.tokens -> {
                            sendMessageToPlayer(player, Message(MessageType.SERVER_RESPONSE, "You don't have enough tokens, going all-in"))
                            betManager.makeBet(player, player.tokens)
                            makeBetToService(player.name, player.tokens,"All in")

                        }
                        amountToCall > 0 -> {
                            betManager.makeBet(player, amountToCall)
                            broadcast(Message(MessageType.SERVER_RESPONSE, "'${player.name}' called with $amountToCall"))
                            makeBetToService(player.name, amountToCall,"Call")

                        }
                        else -> {
                            broadcast(Message(MessageType.SERVER_RESPONSE, "'${player.name}' checked"))
                            makeBetToService(player.name, 0,"Check")

                        }
                    }

                }else if (action.action == BetAction.FOLD){

                    //activePlayers.remove(player)
                    player.hasFolded = true
                    broadcast(Message(MessageType.SERVER_RESPONSE, "'${player.name}' has retired"))
                    makeBetToService(player.name, 0,"Fold")

                }

                if (checkIfPassRound()){
                    newTurn()
                }

                if (player.tokens <= 0){
                    //activePlayers.remove(player)
                    player.playerState = PlayerState.ALL_IN
                }

                nextPlayerIndex()

            }else{
                sendMessageToPlayer(player,Message(MessageType.SERVER_RESPONSE,"Is not your turn"))
            }


        }else{
            sendMessageToPlayer(player,Message(MessageType.SERVER_RESPONSE,"You cant make bets right now"))
        }

    }

    // envia un mensaje a un jugador solo
    private fun sendMessageToPlayer(player: Player, message: Message){
        val jsonMsg = Json.encodeToString(message)
        player.session?.sendMessage(TextMessage(jsonMsg))
    }

    // envia un mensaje a todos los jugadores
    private fun broadcast(message: Message) {
        val jsonMessage = Json.encodeToString<Message>(message)
        players.forEach { it.session?.sendMessage(TextMessage(jsonMessage)) }
    }

}
