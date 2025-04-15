package com.pruebas.repository

import com.pruebas.model.Game
import org.springframework.data.mongodb.repository.MongoRepository

interface GameRepository : MongoRepository<Game, String> {

}