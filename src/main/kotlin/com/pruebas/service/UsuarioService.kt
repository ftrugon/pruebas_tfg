package com.pruebas.service

import com.pruebas.error.exceptions.AlreadyExistException
import com.pruebas.dto.RegistrarUsuarioDTO
import com.pruebas.dto.UsuarioDTO
import com.pruebas.error.exceptions.NotFoundException
import com.pruebas.error.exceptions.UnauthorizedException
import com.pruebas.model.Usuario
import com.pruebas.repository.UsuarioRepository
import com.pruebas.utils.DTOParser
import jdk.jfr.DataAmount
import org.apache.coyote.BadRequestException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.password.PasswordEncoder

@Service
class UsuarioService : UserDetailsService {

    @Autowired
    private lateinit var usuarioRepository: UsuarioRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    override fun loadUserByUsername(username: String?): UserDetails {

        val usuario: Usuario = usuarioRepository
            .findByUsername(username!!)
            .orElseThrow {
                UnauthorizedException("$username no existente")
            }

        return User.builder()
            .username(usuario.username)
            .password(usuario.password)
            .roles(usuario.roles)
            .build()
    }

    fun insertUser(registrarUsuarioDTO: RegistrarUsuarioDTO):UsuarioDTO{

        // COMPROBACIONES
        // campos
        if (registrarUsuarioDTO.username.isBlank()
            || registrarUsuarioDTO.password.isBlank()
            || registrarUsuarioDTO.passwordRepeat.isBlank()) {
            throw BadRequestException("Uno o más campos vacíos")
        }

        if (registrarUsuarioDTO.password.length < 6) {
            throw BadRequestException("La contraseña no puede ser menor de 6 caracteres")
        }

        if (registrarUsuarioDTO.password != registrarUsuarioDTO.passwordRepeat) {
            throw BadRequestException("La contraseña no coincide")
        }

        // existe
        if(usuarioRepository.findByUsername(registrarUsuarioDTO.username).isPresent) {
            throw AlreadyExistException("Usuario ${registrarUsuarioDTO.username} ya está registrado")
        }

        // meter al usuario en la bd
        val usuario = DTOParser.registrarDTOToUsuario(registrarUsuarioDTO)
        usuario.password = passwordEncoder.encode(registrarUsuarioDTO.password)
        usuarioRepository.save(usuario)

        return DTOParser.usuarioToDto(usuario)

    }

    fun getByUsername(username: String): Usuario {
        val user = usuarioRepository.findByUsername(username)

        if (!user.isPresent){
            throw NotFoundException("User $username not found")
        }

        return user.get()
    }

    fun addTokensToUser(username: String,amount: Int): UsuarioDTO {
        val user = getByUsername(username)

        user.tokens += amount

        return DTOParser.usuarioToDto(usuarioRepository.save(user))

    }


    fun retireTokensToUser(username: String,amount: Int): UsuarioDTO {
        val user = getByUsername(username)

        if (amount > user.tokens) {
            throw BadRequestException("You cant retire more amount of tokens than are in your account")
        }else{
            user.tokens -= amount
            return DTOParser.usuarioToDto(usuarioRepository.save(user))
        }

    }


    fun banUser(
        username: String,
    ): Usuario{
        val user = getByUsername(username)
        user.isBanned = true
        return usuarioRepository.save(user)
    }

}