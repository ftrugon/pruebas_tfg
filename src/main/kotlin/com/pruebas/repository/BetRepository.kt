package com.pruebas.repository

import com.pruebas.model.BetDocument
import org.springframework.data.mongodb.repository.MongoRepository

interface BetRepository: MongoRepository<BetDocument, String> {

    fun findAllByUserId(userId: String): List<BetDocument>

}