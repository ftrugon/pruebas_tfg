package com.pruebas.controller

import com.pruebas.dto.RegistrarUsuarioDTO
import com.pruebas.dto.UsuarioDTO
import com.pruebas.dto.UsuarioLoginDTO
import com.pruebas.error.exceptions.AlreadyExistException
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


    /**
     * funcion para registrarse
     */
    @PostMapping("/register")
    fun insert(
        @RequestBody registrarUsuario: RegistrarUsuarioDTO,
    ): ResponseEntity<UsuarioDTO?>? {
        return ResponseEntity(usuarioService.insertUser(registrarUsuario), HttpStatus.CREATED)

    }


    /**
     * funcion para hacer login
     */
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

        if (usuarioService.getByUsername(authentication.name).isBanned) {
            throw UnauthorizedException("The user you are trying to access with is banned")
        }

        val token = tokenService.generarToken(authentication)
        return ResponseEntity(mapOf("token" to token), HttpStatus.OK)
    }


    /**
     * funcion para ver lo detalles de tu cuenta
     */
    @GetMapping("/myInfo")
    fun getUserInfo(
        authentication: Authentication
    ): ResponseEntity<Usuario> {
        return ResponseEntity(usuarioService.getByUsername(authentication.name), HttpStatus.OK)
    }

    /**
     * funcion para obtener los detalles de la cuenta de un usuario siendo admin
     */
    @GetMapping("/getUserInfo/{username}")
    fun getInfoFromUser(
        authentication: Authentication,
        @PathVariable username: String,
    ): ResponseEntity<Usuario> {
        if (authentication.authorities.any { it.authority == "ROLE_ADMIN" }) {
            return ResponseEntity(usuarioService.getByUsername(username),HttpStatus.OK)
        }else{
            throw UnauthorizedException("Admin is required")
        }
    }

    /**
     * funcion para cambiarte el usename
     */
    @PutMapping("/changeUsername/{username}")
    fun changeUsername(
        authentication: Authentication,
        @PathVariable username: String,
    ): ResponseEntity<UsuarioDTO> {

        return ResponseEntity(usuarioService.changeUsername(authentication.name,username),HttpStatus.OK)
    }

    /**
     * funcion para que un admin pueda banear a un usuario
     */
    @PutMapping("/banUser/{username}")
    fun banUser(
        authentication: Authentication,
        @PathVariable username: String,
    ): ResponseEntity<Usuario> {
        if (authentication.authorities.any { it.authority == "ROLE_ADMIN" }) {
            return ResponseEntity(usuarioService.banUser(username),HttpStatus.OK)
        }else{
            throw UnauthorizedException("Admin is required")
        }
    }

}