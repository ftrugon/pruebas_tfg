package com.pruebas

import org.springframework.web.socket.*
import org.springframework.web.socket.handler.TextWebSocketHandler
import org.springframework.stereotype.Component

@Component
class PokerWebSocketHandler : TextWebSocketHandler() {

    private val players = mutableListOf<WebSocketSession>()
    private var currentPlayerIndex = 0

    override fun afterConnectionEstablished(session: WebSocketSession) {
        players.forEach {
            println(it)
        }
        players.add(session)
        println("Jugador conectado: ${session.id}")

        if (players.size == 2) { // Por ahora, mínimo 2 jugadores para empezar
            startGame()
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        println("Conexion cerrada: ${session.id}")
        players.remove(session)
        super.afterConnectionClosed(session, status)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val action = message.payload
        println("Acción recibida: $action")

        if (session == players[currentPlayerIndex]) {
            broadcast("Jugador ${currentPlayerIndex + 1} hizo: $action")
            nextTurn()
        } else {
            session.sendMessage(TextMessage("No es tu turno"))
        }
    }

    private fun startGame() {
        broadcast("Juego iniciado con ${players.size} jugadores")
        notifyTurn()
    }

    private fun nextTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size
        notifyTurn()
    }

    private fun notifyTurn() {
        broadcast("Turno del jugador ${currentPlayerIndex + 1}")
    }

    private fun broadcast(message: String) {
        players.forEach { it.sendMessage(TextMessage(message)) }
    }
}
