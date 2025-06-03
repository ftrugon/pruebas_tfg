package com.pruebas.repository

import com.pruebas.model.BetDocument
import org.springframework.data.mongodb.repository.MongoRepository


/**
 * clase que hereda de mongorepository, sirve para DAO ya que nos permiter realizar acciones entre el codigo y la base de datos
 * en este caso es la de apuestas
 */
interface BetRepository: MongoRepository<BetDocument, String> {

    fun findAllByUserId(userId: String): List<BetDocument>

}