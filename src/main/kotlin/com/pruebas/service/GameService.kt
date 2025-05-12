package com.pruebas.service

import com.pruebas.model.Game
import com.pruebas.repository.GameRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GameService {

    @Autowired
    private lateinit var gameRepository: GameRepository

    fun getAllGames(): List<Game>{
        return gameRepository.findAll()
    }


}