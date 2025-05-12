package com.pruebas.service

import com.pruebas.error.exceptions.AlreadyExistException
import com.pruebas.dto.RegistrarUsuarioDTO
import com.pruebas.dto.UsuarioDTO
import com.pruebas.error.exceptions.UnauthorizedException
import com.pruebas.model.Usuario
import com.pruebas.repository.UsuarioRepository
import com.pruebas.utils.UsuarioDTOParser
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
        val usuario = UsuarioDTOParser.registrarDTOToUsuario(registrarUsuarioDTO)
        usuario.password = passwordEncoder.encode(registrarUsuarioDTO.password)
        usuarioRepository.save(usuario)

        return UsuarioDTOParser.usuarioToDto(usuario)

    }

}