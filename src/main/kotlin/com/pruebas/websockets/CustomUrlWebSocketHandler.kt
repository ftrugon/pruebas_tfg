package com.pruebas.websockets

import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession


/**
 * Clase intermedia para gestionar las conexiones que le llegan al servidor
 * @property games es el bean de games de las paartidas, asocia una id de mesa con un [PokerWebSocketHandler]
 */
class CustomUrlWebSocketHandler(
    private val games: MutableMap<String, PokerWebSocketHandler>
) : WebSocketHandler {

    /**
     * funcion para pillar la id de la mesa de la url
     * @param url la url de la que extraer la id
     * @return la id de la mesa si es que la encuentra o nulo si no encuentra nada
     */
    private fun getTableIdFromUrl(url: String): String? {
        val idTable = url.split("/").lastOrNull()
        return idTable
    }

    /**
     * funcion para buscar y usar la funcion del handler dee la mesa
     * @param session la sesion que qujiere hacer conexion
     */
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

    /**
     * funcion para recibir un mensaje
     * @param session la sesion que envia el mensage
     * @param message el mensage
     */
    override fun handleMessage(session: WebSocketSession, message: WebSocketMessage<*>) {
        val url = session.uri?.path ?: return
        val gameId = getTableIdFromUrl(url) ?: return
        games[gameId]?.handleMessage(session, message)
    }
    /**
     * funcion para recibir un mensaje
     * @param session la sesion que envia el mensage
     * @param exception el error dee porque se ha cerrado la conexion
     */
    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        val url = session.uri?.path ?: return
        val gameId = getTableIdFromUrl(url) ?: return
        games[gameId]?.handleTransportError(session, exception)
    }
    /**
     * funcion para recibir un mensaje
     * @param session la sesion que envia el mensage
     * @param status porque se ha cerraddo la conexion
     */
    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val url = session.uri?.path ?: return
        val gameId = getTableIdFromUrl(url) ?: return
        games[gameId]?.afterConnectionClosed(session, status)
    }

    /**
     * idica si se pueeden usar mensages parciales en este handler, lo pongo en false porque quiero que los mensajes me vengan siempre completos
     */
    override fun supportsPartialMessages(): Boolean = false
}
