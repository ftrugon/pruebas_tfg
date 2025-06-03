package com.pruebas.service

import com.pruebas.dto.InsertTableDTO
import com.pruebas.error.exceptions.NotFoundException
import com.pruebas.model.Table
import com.pruebas.repository.TableRepository
import com.pruebas.utils.DTOParser
import org.apache.coyote.BadRequestException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


/**
 * clase que contiene la logiuca de negocio de las tablas
 */
@Service
class TableService {

    @Autowired
    private lateinit var tableRepository: TableRepository

    /**
     * funcion para insertar una mesa
     * @param tableToInsert los datos de la tabla que queremos insertar
     */
    fun insertTable(tableToInsert: InsertTableDTO): Table{

        if (tableToInsert.title.isEmpty()){
            throw BadRequestException("Title cannot be empty")
        }

        if (tableToInsert.bigBlind < 2){
            throw BadRequestException("Big blind cannot be less than 2")
        }

        val table = DTOParser.insertTableDTOToTable(tableToInsert)
        return tableRepository.save(table)
    }

    /**
     * funcion para obtener todas las mesas actuales que tiene la base de datos
     * @return la lista de todas las mesas
     */
    fun getAllTables(): List<Table>{
        return tableRepository.findAll()
    }

    /**
     * funcion para sumar 1 a los jugadores actuales de una mesa
     * @param tableId la id de la mesa a cambiar
     * @return la mesa con los cambios
     */
    fun sumOneNumOfPlayerFromTable(tableId: String):Table{

        val existTable = tableRepository.findBy_id(tableId)

        if (!existTable.isPresent){
            throw NotFoundException("The table to add a player is not found")
        }

        val table = existTable.get()

        if (table.numPlayers >= 6){
            throw BadRequestException("The table is already full")
        }

        table.numPlayers += 1
        return tableRepository.save(table)
    }

    /**
     * funcion para quitar 1 a los jugadores actuales de una mesa
     * @param tableId la id de la mesa a cambiar
     * @return la mesa con los cambios
     */
    fun subOneNumOfPlayerFromTable(tableId: String):Table{

        val existTable = tableRepository.findBy_id(tableId)

        if (!existTable.isPresent){
            throw NotFoundException("The table to remove a player is not found")
        }

        val table = existTable.get()

        if (table.numPlayers < 0){
            throw BadRequestException("The table is already empty")
        }

        table.numPlayers -= 1
        return tableRepository.save(table)
    }

    /**
     * funcion para eliminar las mesas inutiles
     * @param tablesToDel las mesas que se van a eliminar
     */
    fun deleteTables(tablesToDel: List<Table>){

        for (table in tablesToDel){
            tableRepository.deleteById(table._id!!)
        }

    }


}