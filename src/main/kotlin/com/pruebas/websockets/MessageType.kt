package com.pruebas.websockets


/**
 * enum class para el tipo de mensajes que puedem llegaar o ser enviados
 */
enum class MessageType {
    PLAYER_READY,
    PLAYER_JOIN,
    PLAYER_LEAVE,
    PLAYER_INFO,
    TEXT_MESSAGE,
    ACTION,
    STATE_UPDATE,
    PLAYER_CARDS,
    START_ROUND,
    END_ROUND,
    SERVER_RESPONSE,
    NOTIFY_TURN,
    HAND_RANKING,
    NOTIFY_WINNER,
    SEND_PLAYER_DATA
}