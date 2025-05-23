package com.pruebas.service

import com.pruebas.PokerWebSocketHandler
import com.pruebas.model.Table
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class TableCleaner(

    private val tableService: TableService,
    private val games :MutableMap<String, PokerWebSocketHandler>
) : CommandLineRunner {

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
