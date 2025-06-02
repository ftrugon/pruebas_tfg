package com.pruebas.websockets

import com.pruebas.pokerLogic.BetAction
import com.pruebas.pokerLogic.BetPayload
import com.pruebas.model.BetDocument
import com.pruebas.pokerLogic.BetManager
import com.pruebas.pokerLogic.Card
import com.pruebas.pokerLogic.Deck
import com.pruebas.pokerLogic.HandManager
import com.pruebas.pokerLogic.Player
import com.pruebas.pokerLogic.PlayerState
import com.pruebas.pokerLogic.PotManager
import com.pruebas.pokerLogic.TurnType
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


/**
 * clase que controla el websocket a tiempo real, extiende de [TextWebSocketHandler] para poder realizar esta tarea
 *
 * @property tableTitle nombre de la mesa
 * @property tableId Identificador único de la mesa
 * @property bigBlindAmount valor del big blind para esta mesa
 * @property saldoService service para manejar el saldo de los jugadores
 * @property tableService service para manejar operaciones relacionadas con las mesas
 * @property betService service encargado de las operaciones de apuestas
 */

class PokerWebSocketHandler(
    private val tableTitle: String,
    private val tableId: String,
    private val bigBlindAmount: Int,
    private var saldoService: UsuarioService,
    private var tableService: TableService,
    private var betService: BetService
) : TextWebSocketHandler() {

    private val players = mutableListOf<Player>() // todos los jugadores, incluytendo los de la lobby
    private var playersToKick = mutableListOf<Player>() // jugadores que tienen que ser expulsados al final de la ronda, yaa sea porque no tienen mas ddinero que apostar o se han desconectaddo antes
    private var activePlayers = mutableListOf<Player>() // jugadores activos de la partida
    private var handManager = HandManager() // instancia de hand manager, para evaluar las manos
    private var betManager = BetManager() // instancia de betmanager, para las apuestas dde la partida
    private var potManager = PotManager(betManager) // pot para gestionar los pots
    private var deck = Deck() // baraja de la mesa
    private var communityCards = mutableListOf<Card>() // cartas comunitarias
    private var gameActive = false // booleanop para saber si la partida esta activa
    private var dealerIndex = 0 // index del jugador que es el dealer, de aqui se saca el small y el big blind
    private var actualPlayerIndex = dealerIndex + 1 // jugador actual

    private var smallBlindAmount = bigBlindAmount / 2 // cuanto cuesta el small blind


    private var actualTurn = TurnType.PRE_FLOP // turno actual dde la partida

    /**
     * funcion del websocket de cuando se establece una conexion
     * @param session la session que se conecta
     */
    override fun afterConnectionEstablished(session: WebSocketSession) {

        // en mi proyecto, no pueden haber mas de 6 jugadores en una misma mesa
        if (playersToKick.size < 6) {
            players.add(Player(session,"",0))
        }else {
            afterConnectionClosed(session, CloseStatus(4002,"No space on the table"))
        }
    }

    /**
     * funcion que se ejecuta al principio de cada ronda, para asignar los blinds y el ddealer, tambien limpia y mezcla la baraja
     */
    private fun assignRoles(){


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

    /**
     * reparte las cartas a cada usuario
     */
    private fun giveCards(){
        players.forEach {
            val card1 = deck.drawCard()
            val card2 = deck.drawCard()
            it.giveCard(card1)
            it.giveCard(card2)

            val jsonListOfCards = Json.encodeToString<MutableList<Card>>(it.cards)
            val messageToSend = Message(MessageType.PLAYER_CARDS, jsonListOfCards)
            val jsonMesagge = Json.encodeToString(messageToSend)
            it.session?.sendMessage(TextMessage(jsonMesagge))
        }

    }

    /**
     * funcion que se ejecuta al principio de cada partida, reparte los blinds y los turnos
     */
    private fun betBlinds(){

        // hacer el small blindd
        val smallBlindPlayer = activePlayers.find { it.isSmallBlind }

        if(smallBlindPlayer == null){
            throw NullPointerException("Player not found")
        }
        broadcast(
            Message(
                MessageType.SERVER_RESPONSE,
                "${smallBlindPlayer.name} was small blind, has bet $smallBlindAmount"
            )
        )
        betManager.makeBet(smallBlindPlayer,smallBlindAmount)
        makeBetToService(smallBlindPlayer.name,smallBlindAmount,"Small blind")

        // hacer el big blind
        val bigBlindPlayer = activePlayers.find { it.isBigBlind }
        if(bigBlindPlayer == null){
            throw NullPointerException("Player not found")
        }
        broadcast(Message(MessageType.SERVER_RESPONSE, "${bigBlindPlayer.name} was big blind, has bet $bigBlindAmount"))
        betManager.makeBet(bigBlindPlayer,bigBlindAmount)
        makeBetToService(bigBlindPlayer.name,bigBlindAmount,"Big blind")

        // notificar el turno
        actualPlayerIndex += 3
        val player = activePlayers[actualPlayerIndex % activePlayers.size]
        broadcast(Message(MessageType.NOTIFY_TURN, "${player.name}:${player.tokens}"))

    }

    /**
     * envia la informacion de los jugadores y sus tokens, es una funcion para hacer que en el cliente aparezcan los jguadores con sus tokens
     * @param inLobby para señalar si estan en partida o no, si estan en partida solo mandara la infomacion de activeplayers
     */
    private fun sendInfoOfPlayers(inLobby: Boolean = false){
        val listToSend = mutableListOf<PlayerDataToShow>()

        if (inLobby){
            players.forEach {
                listToSend.add(PlayerDataToShow(it.name, it.tokens))
            }
        }else{
            activePlayers.forEach {
                listToSend.add(PlayerDataToShow(it.name, it.tokens))
            }
        }

        val msgJson = Json.encodeToString<List<PlayerDataToShow>>(listToSend)
        broadcast(Message(MessageType.SEND_PLAYER_DATA, msgJson))
    }

    /**
     * funcion para empezar una ronda
     */
    private fun startRound(){

        gameActive = true
        activePlayers = players.toMutableList()
        assignRoles()
        giveCards()

        sendInfoOfPlayers()

        broadcast(Message(MessageType.START_ROUND, ""))
        betBlinds()


    }

    /**
     * reparte cartas a las cartas cominutarias y les manda a los usuairos cuales son
     * @param numCards el numero de cartas que se van a sacar
     */
    private fun addToCommunityCards(numCards: Int){
        for (i in 0..<numCards){
            communityCards.add(deck.drawCard())
        }

        val jsonListOfCards = Json.encodeToString<MutableList<Card>>(communityCards)

        broadcast(Message(MessageType.STATE_UPDATE, jsonListOfCards))
    }

    /**
     * calcula las manos de cada jugador
     */
    private fun calculateHands(){
        activePlayers.forEach {player ->
            player.hand = handManager.calculateHand((player.cards + communityCards).sortedByDescending { it.value.weight })

            sendMessageToPlayer(player, Message(MessageType.HAND_RANKING, player.hand?.ranking.toString()))
        }
    }

    /**
     * comprueba si todos los jugadores estan listos para jugar
     */
    private fun allPlayersReady(): Boolean {
        players.forEach {
            if (!it.isReadyToPlay) return false
        }
        return true
    }

    /**
     * funcion para cuando se desconectee un usuario, mandar la informacion de sus tokens a la base de datos
     */
    private fun disconnectUser(player: Player?){
        try {
            saldoService.addTokensToUser(player?.name ?: "" ,player?.tokens ?: 0)
            tableService.subOneNumOfPlayerFromTable(tableId)
        }catch (e: Exception){
            println("Hubo una excepcion al persistir datos del usuario que se ha cerrado")
        }
    }

    /**
     * funcion por defecto de la clase que hereda de websocket, controla si el jugador que se desconecta esta jugando ahora mismo
     * @param session la sesion del usuario que se quiere ddesconectar
     * @param status el porque se ha desconectaddo el usuario
     */
    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {

        // mira el jugador que cierra la sesion
        val sessionPlayer = players.find { it.session == session }

        // si la partida esta activa y el jugador esta dentro
        if (gameActive && (sessionPlayer in activePlayers)) {
            sessionPlayer?.session = null
            sessionPlayer?.hasFolded = true

            if (sessionPlayer == players[actualPlayerIndex % players.size]){
                nextPlayerIndex()
            }

            if (numOfPlayersFolded() == activePlayers.size){
                endRound()
            }
        }else {

            players.remove(sessionPlayer)

            disconnectUser(sessionPlayer)

            broadcast(Message(MessageType.PLAYER_LEAVE, sessionPlayer?.name ?: ""))
            sendInfoOfPlayers(true)
            super.afterConnectionClosed(session, status)

        }

    }

    /**
     * funcion para cuando un jugador se une a la mesa, el usuario manda automaticamente este mensaje para almacenar su informacion
     * @param player el jugador que se une
     * @param msgPayload la informaacion del usuario en forma de string
     */
    private fun getPlayerInfo(player: Player,msgPayload: String){
        val playerInfo = Json.decodeFromString<PlayerInfoMessage>(msgPayload)

        try{
            player.name = playerInfo.name
            player.tokens = playerInfo.dinero

            saldoService.retireTokensToUser(player.name,playerInfo.dinero)
            tableService.sumOneNumOfPlayerFromTable(tableId)

            val playerDataToShowString =  Json.encodeToString(PlayerDataToShow(player.name, player.tokens))
            broadcast(Message(MessageType.PLAYER_JOIN, playerDataToShowString))
            sendInfoOfPlayers(true)

        }catch(e: Exception){
            afterConnectionClosed(player.session!!, CloseStatus(4001,e.message))
        }

    }

    /**
     * comprueba si un jugador esta listo y si las condiciones se dan comienza la partida
     * @param player el jugador que manda la accion
     */
    private fun playerReady(player: Player){
        // Si la partida esta activa, no se puede dar a listo directamente
        if (!gameActive){
            player.isReadyToPlay = !player.isReadyToPlay
            val jsonToBool = Json.encodeToString<Pair<String, Boolean>>(player.name to player.isReadyToPlay)

            broadcast(Message(MessageType.PLAYER_READY, jsonToBool))

            // El poker requiere minimo de 2 personas para comenzar la partida, si no hay 2 personas en la lobby
            if (players.size >= 2 && allPlayersReady()) {
                broadcast(Message(MessageType.SERVER_RESPONSE, "The game is going to start!"))
                startRound()
            }
        }else{
            sendMessageToPlayer(player, Message(MessageType.SERVER_RESPONSE, "There is an active game right now"))
        }
    }

    /**
     * funcion por defecto de websocket para recibir un mensaje, lo convierto a json para parsear el mensaje con una clase personalizada y dependiendo de este hago una cosa u otra
     * @param session la sesion del que manda el mensaje
     * @param message el payload del mensaje
     */
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
                val msgToSend = Message(MessageType.TEXT_MESSAGE, "${playerToChange.name}: ${msg.content}")
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

    /**
     * comprueba si tiene que ir a la siguiente ronda
     * @return un booleano para saber si hay que pasar la rondad
     */
    private fun checkIfPassRound(): Boolean{
        return activePlayers
            .filter { it.playerState != PlayerState.ALL_IN && !it.hasFolded }
            .all { it.playerState == PlayerState.READY }
    }

    /**
     * pone todos los jugadores a not ready, por si alguien hace raise
     */
    private fun allPlayersToNotReady(){
        activePlayers.forEach {
            if (it.playerState != PlayerState.ALL_IN && !it.hasFolded) {
                it.playerState = PlayerState.NOT_READY
            }
        }
    }

    /**
     * comprueba los ganadroes de la ronda actual
     */
    private fun chooseWinners(){

        // side pots con el pot manager
        val sidePots = potManager.calculateSidePots()

        for (i in sidePots.indices) {
            // para cada pot calcula los ganaddores
            val winners = handManager.compareHands(sidePots[i].players)
            val prize = sidePots[i].amount / winners.size
            winners.forEach {
                broadcast(
                    Message(
                        MessageType.NOTIFY_WINNER,
                        "${it.name} es un gandor del pot ${i + 1} y se lleva $prize"
                    )
                )
                it.tokens += prize
            }
        }

    }

    /**
     * funcion para acabar la ronda, lo reinicia todo
     */
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
            // si ya se ha desconectado, quiere decir que se ha desconectado en medio de la partida y no se le han devuelto los tokens
            if (it.session == null){
                disconnectUser(it)
            }
            if (it.session != null){
                afterConnectionClosed(it.session!!, CloseStatus(4001,""))
            }
        }

        players.removeAll(playersToKick)

        communityCards.clear()

        actualTurn = TurnType.PRE_FLOP
        betManager.clear()
        betManager.resetBets(players)
        gameActive = false
        broadcast(Message(MessageType.END_ROUND, ""))
        sendInfoOfPlayers(true)
    }

    /**
     * funcion para saber el numero de jugaddores que han foldeaddo
     * @return el numero dde jugadores que ha foldeado
     */
    private fun numOfPlayersFolded():Int{
        var num = 0

        activePlayers.forEach {
            if (it.hasFolded){
                num++
            }
        }

        return num
    }

    /**
     * nuevo turno despues del preflop, flop ....
     */
    private fun newTurn(){
        // dependiendo de si esta en el preflop, flop o river, sacara 3, 1 o no sacara cartas
        // por el momento paara probar solo voy a sacar 1 carta

        if (actualTurn == TurnType.SHOWDOWN || numOfPlayersFolded() == activePlayers.size - 1){

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

    /**
     * funcion para calcular el proximo index de los jugadores
     */
    private fun nextPlayerIndex() {
        if (numOfPlayersFolded() == activePlayers.size){
            actualPlayerIndex = 0
            endRound()
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

    /**
     * Envia una apuesta a la base de datos
     * @param playerName el nombre del jugador
     * @param amount la  cantidad dde la apuesta
     * @param betType el tipo de apuesta
     */
    fun makeBetToService(playerName: String, amount: Int, betType: String){
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = saldoService.getByUsername(playerName)._id
                betService.insertBet(BetDocument(null,tableTitle,tableId,userId?: "",amount,betType))
            }catch (e:Exception){
                println("Error while uploading bet on db")
            }
        }
    }

    /**
     * funcion para recibir la accion que ha hecho el jugador
     * @param player el jugador que hace la accion
     * @param action la accion que ha hecho el jugador
     */
    private fun receiveBetAction(player: Player, action: BetPayload){

        if (gameActive){

            if (player == activePlayers[actualPlayerIndex % activePlayers.size]){

                if (action.action == BetAction.RAISE && action.amount > 0){
                    // mira cual es la maxima apuesta de los jugaodres
                    val maxBet = activePlayers.maxOfOrNull { it.currentBet } ?: 0
                    // mira si tiene que poner para callear
                    val amountToCall = maxOf(0, maxBet - player.currentBet)
                    // suma la cantidad para checkear y la cantidad que quiere hacer raise
                    val amountToBet = amountToCall + action.amount

                    if (amountToBet <= player.tokens){
                        // hace la apuesta y pone a todos lo jugadores como no listo
                        broadcast(Message(MessageType.SERVER_RESPONSE, "'${player.name}' raised $amountToBet"))

                        allPlayersToNotReady()

                        player.playerState = PlayerState.READY

                        betManager.makeBet(player,amountToBet)
                        makeBetToService(player.name, amountToBet,"Raise")

                    }else{

                        // hace aallin en caso de que no tengas suficientes tokens
                        sendMessageToPlayer(player,
                            Message(MessageType.SERVER_RESPONSE, "You don't have enough tokens, going all-in")
                        )

                        betManager.makeBet(player,player.tokens)
                        makeBetToService(player.name, amountToBet,"All in")

                        allPlayersToNotReady()

                    }


                }else if (action.action == BetAction.CALL){

                    // casi igual que el raise pero quitando la parte de subir la apuesta

                    player.playerState = PlayerState.READY

                    val maxBet = activePlayers.maxOfOrNull { it.currentBet } ?: 0
                    val amountToCall = maxOf(0, maxBet - player.currentBet)

                    when {
                        amountToCall > player.tokens -> {
                            sendMessageToPlayer(player,
                                Message(MessageType.SERVER_RESPONSE, "You don't have enough tokens, going all-in")
                            )
                            betManager.makeBet(player, player.tokens)
                            makeBetToService(player.name, player.tokens,"All in")

                        }
                        amountToCall > 0 -> {
                            betManager.makeBet(player, amountToCall)
                            broadcast(
                                Message(
                                    MessageType.SERVER_RESPONSE,
                                    "'${player.name}' called with $amountToCall"
                                )
                            )
                            makeBetToService(player.name, amountToCall,"Call")

                        }
                        else -> {
                            broadcast(Message(MessageType.SERVER_RESPONSE, "'${player.name}' checked"))
                            makeBetToService(player.name, 0,"Check")

                        }
                    }

                }else if (action.action == BetAction.FOLD){

                    // caso para foldear
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
                sendMessageToPlayer(player, Message(MessageType.SERVER_RESPONSE, "Is not your turn"))
            }


        }else{
            sendMessageToPlayer(player, Message(MessageType.SERVER_RESPONSE, "You cant make bets right now"))
        }

    }

    /**
     * envia un mensaje a un jugador solo
     * @param player el jugador al que se le enviara el mensaje
     * @param message el mensaje a enviar al jugador
     */
    private fun sendMessageToPlayer(player: Player, message: Message){
        val jsonMsg = Json.encodeToString(message)
        player.session?.sendMessage(TextMessage(jsonMsg))
    }

    /**
     * envia un mensaje a todos los jugadores
     * @param message mensaje para enviar a todos los jugadores
     */
    private fun broadcast(message: Message) {
        val jsonMessage = Json.encodeToString<Message>(message)
        players.forEach { it.session?.sendMessage(TextMessage(jsonMessage)) }
    }

}
