package com.pruebas.repository

import com.pruebas.model.Usuario
import org.springframework.data.mongodb.repository.MongoRepository
import java.util.*

interface UsuarioRepository: MongoRepository<Usuario, String> {
    fun findByUsername(username: String): Optional<Usuario>

}