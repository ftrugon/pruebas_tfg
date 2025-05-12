package com.pruebas.controller

import com.pruebas.model.Game
import com.pruebas.service.GameService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/games")
class GameController {

    @Autowired
    private lateinit var gameService: GameService


    @PostMapping
    fun insert(){

    }

    @GetMapping
    fun getGames():List<Game> {
        return gameService.getAllGames()
    }

    @PutMapping
    fun update(){

    }

    @DeleteMapping
    fun delete(){
        
    }


}