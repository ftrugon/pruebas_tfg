package com.pruebas.repository

import com.pruebas.model.Table
import org.springframework.data.mongodb.repository.MongoRepository
import java.util.Optional

interface TableRepository : MongoRepository<Table, String> {

    fun findBy_id(_id: String): Optional<Table>

}