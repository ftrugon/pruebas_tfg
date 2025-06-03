package com.pruebas.controller

import com.pruebas.error.exceptions.NotFoundException
import com.pruebas.error.exceptions.UnauthorizedException
import com.pruebas.model.BetDocument
import com.pruebas.service.BetService
import com.pruebas.service.UsuarioService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/bets")
class BetController {


    @Autowired
    private lateinit var betService: BetService

    @Autowired
    private lateinit var usuarioService: UsuarioService

    /**
     * funcion para obtener las bets de el usuario que hace la consulta
     */
    @GetMapping("/myBets")
    fun getMyBets(
        authentication: Authentication
    ): ResponseEntity<List<BetDocument>> {

        val user = usuarioService.getByUsername(authentication.name)

        if (user._id == null){
            throw NotFoundException("Main variable from user is null")
        }

        return ResponseEntity(betService.getBetsByUserId(user._id), HttpStatus.OK)

    }

    /**
     * funcion para obtener laas apuestas de un usuario siendo admin
     */
    @GetMapping("/betsFromUser/{userId}")
    fun getBetsByUserId(
        authentication: Authentication,
        @PathVariable userId: String
    ): ResponseEntity<List<BetDocument>> {

        if (authentication.authorities.any { it.authority == "ROLE_ADMIN" }){
            val user = usuarioService.getByUsername(authentication.name)

            if (user._id == null){
                throw NotFoundException("Main variable from user is null")
            }

            return ResponseEntity(betService.getBetsByUserId(user._id), HttpStatus.OK)
        }else{
            throw UnauthorizedException("Admin is required")
        }

    }


}