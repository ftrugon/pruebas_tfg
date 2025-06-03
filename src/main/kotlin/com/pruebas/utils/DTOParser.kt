package com.pruebas.utils

import com.pruebas.dto.InsertTableDTO
import com.pruebas.dto.RegistrarUsuarioDTO
import com.pruebas.dto.UsuarioDTO
import com.pruebas.model.Table
import com.pruebas.model.Usuario

/**
 * objeto para parsear dtos
 */
object DTOParser {

    /**
     * funcion para pasar un dto de registro a un usuario
     * @param registrarUsuarioDTO el dto del registro
     * @return el usuario registrado en forma de usuario
     */
    fun registrarDTOToUsuario(registrarUsuarioDTO: RegistrarUsuarioDTO): Usuario {
        return Usuario(
            null,
            registrarUsuarioDTO.username,
            registrarUsuarioDTO.password,
            0
        )
    }

    /**
     * funcion para pasar un usuario u un dto
     * @param usuario el usuario paara parsear
     * @return el usuario parseado a dto
     */
    fun usuarioToDto(usuario: Usuario): UsuarioDTO {
        return UsuarioDTO(
            usuario.username,
            usuario.tokens
        )
    }

    /**
     * funcion para pasar un table dto a able
     * @param insertTableDTO la mesa a insertar
     * @return la mesa insertada
     */
    fun insertTableDTOToTable(insertTableDTO: InsertTableDTO): Table {
        return Table(null,
            insertTableDTO.title,
            insertTableDTO.desc,
            0,
            insertTableDTO.bigBlind
        )
    }

}