package com.pruebas.controller

import com.pruebas.dto.UsuarioDTO
import com.pruebas.error.exceptions.UnauthorizedException
import com.pruebas.service.TableService
import com.pruebas.service.UsuarioService
import com.pruebas.utils.DTOParser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/tokens")
class PlayerTokensController {

    @Autowired
    private lateinit var usuarioService: UsuarioService

    // NOTA MENTAL, 100 TOKENS DE USUARIO = 1€, 1 TOKEN = 1 Cent

    /**
     * funcion para obtener la cantidad de tokens que hay en mi cuenta
     */
    @GetMapping("/getTotalAmount")
    fun getTotalAmount(
        authentication: Authentication
    ): ResponseEntity<Int> {

        val user = usuarioService.getByUsername(authentication.name)

        return ResponseEntity(user.tokens, HttpStatus.OK)

    }

    /**
     * funcion para insertar tokens a mi cuenta
     */
    @PutMapping("/insertTokens/{amount}")
    fun insertTokens(
        authentication: Authentication,
        @PathVariable("amount") amount: Int
    ): ResponseEntity<UsuarioDTO> {
        // LLAMAR A OTRA API PARA EL PAGO DE LA TRANSACCION

        return ResponseEntity(usuarioService.addTokensToUser(authentication.name,amount), HttpStatus.OK)

    }


    /**
     * funcion para aretirar tokens de mi cuenta
     */
    @PutMapping("/retireTokens/{amount}")
    fun retireTokens(
        authentication: Authentication,
        @PathVariable("amount") amount: Int
    ): ResponseEntity<UsuarioDTO> {


        usuarioService.retireTokensToUser(authentication.name,amount)

        // LLAMAR A OTRA API PARA RETIRAR EL DINERO

        // otraapi.añadiracuenta(amountToRetire)

        return ResponseEntity( DTOParser.usuarioToDto(usuarioService.getByUsername(authentication.name)),HttpStatus.OK)

    }


    /**
     * fucnion para insertar tokens a la cuenta de un usuario siendo administrador
     */
    @PutMapping("/insertTokensFrom/{username}/{amount}")
    fun insertTokensFromUser(
        authentication: Authentication,
        @PathVariable("amount") amount: Int,
        @PathVariable username: String
    ): ResponseEntity<UsuarioDTO> {
        // LLAMAR A OTRA API PARA EL PAGO DE LA TRANSACCION

        if (authentication.authorities.any { it.authority == "ROLE_ADMIN" }) {
            return ResponseEntity(usuarioService.addTokensToUser(authentication.name,amount), HttpStatus.OK)
        }else{
            throw UnauthorizedException("Admin is required")
        }

    }

    /**
     * funcion para retirar tokens de un usuario siendo administrador
     */
    @PutMapping("/retireTokensFrom/{username}/{amount}")
    fun retireTokensFromUser(
        authentication: Authentication,
        @PathVariable("amount") amount: Int,
        @PathVariable username: String
    ): ResponseEntity<UsuarioDTO> {


        usuarioService.retireTokensToUser(authentication.name,amount)

        // LLAMAR A OTRA API PARA RETIRAR EL DINERO

        // otraapi.añadiracuenta(amountToRetire)

        return ResponseEntity( DTOParser.usuarioToDto(usuarioService.getByUsername(authentication.name)),HttpStatus.OK)

    }


}