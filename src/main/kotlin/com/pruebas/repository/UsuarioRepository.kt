package com.pruebas.repository

import com.pruebas.model.Usuario
import org.springframework.data.mongodb.repository.MongoRepository
import java.util.*

/**
 * clase que hereda de mongorepository, sirve para DAO ya que nos permiter realizar acciones entre el codigo y la base de datos
 * en este caso es la de usuario
 */
interface UsuarioRepository: MongoRepository<Usuario, String> {
    fun findByUsername(username: String): Optional<Usuario>

}