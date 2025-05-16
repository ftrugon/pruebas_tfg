package com.pruebas

import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry


@Configuration
@EnableWebSocket
class WebSocketConfig : WebSocketConfigurer {

    private val games = mutableMapOf<String, PokerWebSocketHandler>()

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(CustomUrlWebSocketHandler(games), "/game/{gameid}/{bigBlind}")
            .setAllowedOrigins("*")
    }

}