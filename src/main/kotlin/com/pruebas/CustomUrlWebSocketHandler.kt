package com.pruebas

import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession
import kotlin.text.get


class CustomUrlWebSocketHandler(
    private val games: MutableMap<String, PokerWebSocketHandler>
) : WebSocketHandler {

    private fun getTableIdFromUrl(url: String): String? {
        val listOfParams = url.split("/")

        try {
            return listOfParams[listOfParams.size - 1]
        }catch (e:Exception){
            print("gameId did not exist on url")
            return null
        }
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

        val handler = games[gameId]

        if (handler == null) {
            // No existe esa mesa
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Mesa no encontrada"))
            return
        }

//        val bigBlindAmount = getBigBlindFromUrl(url) ?: return
//        val handler = games.getOrPut(gameId) { PokerWebSocketHandler(gameId,bigBlindAmount) }
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
