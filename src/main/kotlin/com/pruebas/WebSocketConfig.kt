package com.pruebas

import com.pruebas.service.TableService
import com.pruebas.service.UsuarioService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry


//@Configuration
//@EnableWebSocket
//class WebSocketConfig : WebSocketConfigurer {
//
//    private val games = mutableMapOf<String, PokerWebSocketHandler>()
//
//    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
//        registry.addHandler(CustomUrlWebSocketHandler(games), "/game/{gameid}/{bigBlind}")
//            .setAllowedOrigins("*")
//    }
//
//}


@Configuration
@EnableWebSocket
class WebSocketGameConfig : WebSocketConfigurer {

    @Bean
    fun gamesMap(): MutableMap<String, PokerWebSocketHandler> {
        return mutableMapOf()
    }

    @Bean
    fun webSocketHandler(gamesMap: MutableMap<String, PokerWebSocketHandler>): CustomUrlWebSocketHandler {
        return CustomUrlWebSocketHandler(gamesMap)
    }

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(webSocketHandler(gamesMap()), "/game/{gameid}")
            .setAllowedOrigins("*")
    }
}