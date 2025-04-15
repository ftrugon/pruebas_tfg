package com.pruebas.utils

import com.pruebas.dto.RegistrarUsuarioDTO
import com.pruebas.dto.UsuarioDTO
import com.pruebas.model.Usuario

object UsuarioDTOParser {

    fun registrarDTOToUsuario(registrarUsuarioDTO: RegistrarUsuarioDTO): Usuario {
        return Usuario(
            null,
            registrarUsuarioDTO.username,
            registrarUsuarioDTO.password,
            registrarUsuarioDTO.email,
            registrarUsuarioDTO.rol
        )
    }

    fun usuarioToDto(usuario: Usuario): UsuarioDTO {
        return UsuarioDTO(
            usuario.username,
            usuario.email
        )
    }

}