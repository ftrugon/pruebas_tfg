package com.pruebas

import com.pruebas.pokerLogic.BetManager
import com.pruebas.pokerLogic.Deck
import com.pruebas.pokerLogic.HandManager
import com.pruebas.pokerLogic.Player
import com.pruebas.pokerLogic.PotManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.socket.*
import org.springframework.web.socket.handler.TextWebSocketHandler
import org.springframework.stereotype.Component

@Component
class PokerWebSocketHandler : TextWebSocketHandler() {

    private val players = mutableListOf<Player>()
    private var currentPlayerIndex = 0
    private var handManager = HandManager()
    private var betManager = BetManager()
    private var potManager = PotManager(betManager)
    private var deck = Deck()

    override fun afterConnectionEstablished(session: WebSocketSession) {

        players.forEach {
            println(it)
        }

        players.add(Player(session,"",0))
        println("Jugador conectado: ${session.id}")

        broadcast(Message(MessageType.PLAYER_JOIN, ""))

        if (players.size == 2 && allPlayersReady()) {
            // METODO PARA EMPEZAR LA PARTIDA O LO QUE SEA
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

            println(playerToChange)

        }


        // TODO LIST --> EL RESTO DE CASO DEL TIPO DE LOS MENSAJES,

        // PLAYER_READY --> Notificar al resto de los usuarios que un usuario esta listo

        if (message.messageType == MessageType.PLAYER_READY){

            playerToChange?.isReady = !playerToChange.isReady

            broadcast(Message(MessageType.PLAYER_READY,Json.encodeToString(playerToChange)))

        }

        // TEXT_MESSAGE, --> Poner en un chat dentro del cliente el mensaje


        if (message.messageType == MessageType.TEXT_MESSAGE){
            broadcast(message)
        }


        // ESTOS 2 PUNTOS SON LOS COMPLICADOS, DE MOMENTO COMINUCACION CON EN CLIETNE

        // ACTION, --> Las distintas acciones que puede hace el usuario (fold, call, raise)
        // STATE_UPDATE --> para cuando el servidor haga algun cambio, notificarselo a los usuarios

        println("Acción recibida: $action")

        if (session == players[currentPlayerIndex].session) {

        } else {
            session.sendMessage(TextMessage("No es tu turno"))
        }
    }


    private fun broadcast(message: Message) {
        val jsonMessage = Json.encodeToString<Message>(message)
        players.forEach { it.session.sendMessage(TextMessage(jsonMessage)) }
    }
}
