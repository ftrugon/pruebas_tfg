package com.pruebas.model

import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "coll_Games")
data class Game(
    val name: String,
) {
}