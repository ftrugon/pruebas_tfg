package com.pruebas

import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession


class CustomUrlWebSocketHandler(
    private val games: MutableMap<String, PokerWebSocketHandler>
) : WebSocketHandler {

    override fun afterConnectionEstablished(session: WebSocketSession) {

        val listOfParams = session.uri?.path?.split("/") ?: return
        val gameId: String
        try {
            gameId = listOfParams[listOfParams.size - 2]
        }catch (e:Exception){
            print("gameId did not exist on url")
            return
        }

        val bigBlindAmount: Int
        try {
            bigBlindAmount = listOfParams[listOfParams.size - 1].toInt()
        }catch (e:Exception){
            print("big blind amount did not exist on url")
            return
        }

        //val gameId = session.uri?.path?.split("/")?.lastOrNull() ?: return

        val handler = games.getOrPut(gameId) { PokerWebSocketHandler(gameId,bigBlindAmount) }
        handler.afterConnectionEstablished(session)
    }

    override fun handleMessage(session: WebSocketSession, message: WebSocketMessage<*>) {
        val gameId = session.uri?.path?.split("/")?.lastOrNull() ?: return
        games[gameId]?.handleMessage(session, message)
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        val gameId = session.uri?.path?.split("/")?.lastOrNull() ?: return
        games[gameId]?.handleTransportError(session, exception)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val gameId = session.uri?.path?.split("/")?.lastOrNull() ?: return
        games[gameId]?.afterConnectionClosed(session, status)
    }

    override fun supportsPartialMessages(): Boolean = false
}
