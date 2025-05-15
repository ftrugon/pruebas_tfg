package com.pruebas.utils

import com.pruebas.dto.InsertTableDTO
import com.pruebas.dto.RegistrarUsuarioDTO
import com.pruebas.dto.UsuarioDTO
import com.pruebas.model.Table
import com.pruebas.model.Usuario

object DTOParser {

    fun registrarDTOToUsuario(registrarUsuarioDTO: RegistrarUsuarioDTO): Usuario {
        return Usuario(
            null,
            registrarUsuarioDTO.username,
            registrarUsuarioDTO.password,
            100
        )
    }

    fun usuarioToDto(usuario: Usuario): UsuarioDTO {
        return UsuarioDTO(
            usuario.username,
            usuario.tokens
        )
    }

    fun insertTableDTOToTable(insertTableDTO: InsertTableDTO): Table {
        return Table(null,
            insertTableDTO.title,
            insertTableDTO.desc,
            0,
            insertTableDTO.bigBlind
        )
    }

    fun tableToTableDTO(){

    }

}