package com.pruebas.controller

import com.pruebas.dto.RegistrarUsuarioDTO
import com.pruebas.dto.UsuarioDTO
import com.pruebas.dto.UsuarioLoginDTO
import com.pruebas.error.exceptions.UnauthorizedException
import com.pruebas.model.Usuario
import com.pruebas.service.TokenService
import com.pruebas.service.UsuarioService
import com.pruebas.utils.DTOParser
import jakarta.servlet.http.HttpServletRequest
import org.apache.coyote.BadRequestException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users")
class UsuarioController {


    @Autowired
    private lateinit var authenticationManager: AuthenticationManager

    @Autowired
    private lateinit var tokenService: TokenService

    @Autowired
    private lateinit var usuarioService: UsuarioService


    @PostMapping("/register")
    fun insert(
        httpRequest: HttpServletRequest,
        @RequestBody registrarUsuario: RegistrarUsuarioDTO,
    ): ResponseEntity<UsuarioDTO?>? {

        return ResponseEntity(usuarioService.insertUser(registrarUsuario), HttpStatus.CREATED)

    }


    @PostMapping("/login")
    fun login(
        @RequestBody usuarioLoginDTO: UsuarioLoginDTO
    ): ResponseEntity<Any> {

        val authentication: Authentication
        try {
            authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(
                    usuarioLoginDTO.username,
                    usuarioLoginDTO.password
                )
            )
        } catch (e: AuthenticationException) {
            throw UnauthorizedException("Credenciales incorrectas")
        }

        // SI PASAMOS LA AUTENTICACIÓN, SIGNIFICA QUE ESTAMOS BIEN AUTENTICADOS
        // PASAMOS A GENERAR EL TOKEN
        val token = tokenService.generarToken(authentication)

        return ResponseEntity(mapOf("token" to token), HttpStatus.OK)
    }


    @GetMapping("/myInfo")
    fun getUserInfo(
        authentication: Authentication
    ): ResponseEntity<Usuario> {
        return ResponseEntity(usuarioService.getByUsername(authentication.name), HttpStatus.OK)
    }

    // NOTA MENTAL, 100 TOKENS DE USUARIO = 1€, 1 TOKEN = 1 Cent

    @GetMapping("/getTotalAmount")
    fun getTotalAmount(
        authentication: Authentication
    ): ResponseEntity<Int> {

        val user = usuarioService.getByUsername(authentication.name)

        return ResponseEntity(user.tokens, HttpStatus.OK)

    }


    @PostMapping("/insertTokens/{amount}")
    fun insertTokens(
        authentication: Authentication,
        @PathVariable("amount") amount: Int
    ): ResponseEntity<UsuarioDTO> {
        // LLAMAR A OTRA API PARA EL PAGO DE LA TRANSACCION

        return ResponseEntity(usuarioService.addTokensToUser(authentication.name,amount), HttpStatus.OK)

    }


    @PostMapping("/retireTokens/{amount}")
    fun retireTokens(
        authentication: Authentication,
        @PathVariable("amount") amount: Int
    ): ResponseEntity<UsuarioDTO> {

        // SABER LA CANTIDAD DE TOKENS A RETIRAR

        val amountToRetire = usuarioService.retireTokensToUser(authentication.name,amount)

        // LLAMAR A OTRA API PARA RETIRAR EL DINERO

        // otraapi.añadiracuenta(amountToRetire)

        return ResponseEntity( DTOParser.usuarioToDto(usuarioService.getByUsername(authentication.name)),HttpStatus.OK)

    }

}