package com.pruebas.repository

import com.pruebas.model.Table
import org.springframework.data.mongodb.repository.MongoRepository
import java.util.Optional


/**
 * clase que hereda de mongorepository, sirve para DAO ya que nos permiter realizar acciones entre el codigo y la base de datos
 * en este caso es la de mesas
 */
interface TableRepository : MongoRepository<Table, String> {

    fun findBy_id(_id: String): Optional<Table>

}