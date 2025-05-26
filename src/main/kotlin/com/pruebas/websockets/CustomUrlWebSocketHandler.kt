package com.pruebas.websockets

import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession


class CustomUrlWebSocketHandler(
    private val games: MutableMap<String, PokerWebSocketHandler>
) : WebSocketHandler {

    private fun getTableIdFromUrl(url: String): String? {
        val idTable = url.split("/").lastOrNull()

        return idTable

    }
//
//    private fun getBigBlindFromUrl(url: String): Int? {
//        val listOfParams = url.split("/")
//        try {
//            return listOfParams[listOfParams.size - 1].toInt()
//        }catch (e:Exception){
//            print("big blind amount did not exist on url")
//            return null
//        }
//    }

    override fun afterConnectionEstablished(session: WebSocketSession) {


        val url = session.uri?.path ?: return
        val gameId = getTableIdFromUrl(url) ?: return
        println("Conexión WebSocket recibida para la mesa: $gameId")

        val handler = games[gameId]

        if (handler == null) {
            println("handler no encontrado")
            // No existe esa mesa
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Mesa no encontrada"))
            return
        }

        handler.afterConnectionEstablished(session)
    }

    override fun handleMessage(session: WebSocketSession, message: WebSocketMessage<*>) {
        val url = session.uri?.path ?: return
        val gameId = getTableIdFromUrl(url) ?: return
        games[gameId]?.handleMessage(session, message)
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        val url = session.uri?.path ?: return
        val gameId = getTableIdFromUrl(url) ?: return
        games[gameId]?.handleTransportError(session, exception)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val url = session.uri?.path ?: return
        val gameId = getTableIdFromUrl(url) ?: return
        games[gameId]?.afterConnectionClosed(session, status)
    }

    override fun supportsPartialMessages(): Boolean = false
}
