package com.pruebas.service

import com.pruebas.websockets.PokerWebSocketHandler
import com.pruebas.model.Table
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

/**
 * clase para eliminar las mesas inutiles al iniciar el servidor
 * @property tableService el service para poder borrar las mesas
 * @property games una bean que tengo para almacenar todas las partidas en caliente
 */
@Component
class TableCleaner(
    private val tableService: TableService,
    private val games :MutableMap<String, PokerWebSocketHandler>
) : CommandLineRunner { // hereda de esta clase para poder ejecutar codigo de primeras con su funcion

    override fun run(vararg args: String?) {

        val tablesToDel = mutableListOf<Table>()
        val allTables = tableService.getAllTables()

        for (table in allTables) {
            if (table._id !in games.keys) {
                tablesToDel.add(table)
            }
        }

        tablesToDel.addAll(allTables.filter {
            it.numPlayers <= 0 && it !in tablesToDel
        })

        tableService.deleteTables(tablesToDel)
        for (table in tablesToDel) {
            games.remove(table._id)
        }

    }
}
