package com.pruebas.service

import com.pruebas.dto.InsertTableDTO
import com.pruebas.error.exceptions.NotFoundException
import com.pruebas.model.Table
import com.pruebas.repository.TableRepository
import com.pruebas.utils.DTOParser
import org.apache.coyote.BadRequestException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TableService {

    @Autowired
    private lateinit var tableRepository: TableRepository

    fun createTable(tableToInsert: InsertTableDTO){
        // Comprobaciones: no hace falta

        val table = DTOParser.insertTableDTOToTable(tableToInsert)
        tableRepository.save(table)
    }

    fun getAllTables(): List<Table>{
        return tableRepository.findAll()
    }

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


    fun deleteEmptyTables () {

        val tablesToDel = tableRepository.findAll().filter { it.numPlayers <= 0 }
        tableRepository.deleteAll(tablesToDel)

    }


}