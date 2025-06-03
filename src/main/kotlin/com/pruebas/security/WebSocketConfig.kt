package com.pruebas.security

import com.pruebas.websockets.CustomUrlWebSocketHandler
import com.pruebas.websockets.PokerWebSocketHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

/**
 * clase para configurar las conexiones a mi servidor mediante websockets
 */
@Configuration
@EnableWebSocket
class WebSocketConfig : WebSocketConfigurer {

    /**
     * funcion para guardar mi mapa de idMesa - pokerhandler
     * es un bean para que springboot lo tome como si fuera un singleton y lo puedo  llamar ddesde casi cualquier parte
     * ya que actuan como si fueran un singleton
     * @return el mapa de idMesa - pokerhandler
      */
    @Bean
    fun gamesMap(): MutableMap<String, PokerWebSocketHandler> {
        return mutableMapOf()
    }

    /**
     * funcion para almacenar el handler que gestiona las conexiones unicas, quiero que todas las conexiones pasen por un mismo embudo
     * por si llega 1 conexion, no quiero que se reparta por distintos de estos ya que puede haber un error de conexion
     * @param gamesMap el mapaa de idMesa - pokerhandler es necesario para crear esta clase
     * @return el handler para conexiones personalizadas
     */
    @Bean
    fun webSocketHandler(gamesMap: MutableMap<String, PokerWebSocketHandler>): CustomUrlWebSocketHandler {
        return CustomUrlWebSocketHandler(gamesMap)
    }

    /**
     * funcion que registra los endpoints del ws, define /game/{gameid} y le dal el handler que he eelegido, en este caso el de la funcion anterior
     * @param registry una herramienta de sprign para controlar las conexiones
     */
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(webSocketHandler(gamesMap()), "/game/{gameid}")
            .setAllowedOrigins("*")
    }
}