package com.pruebas.controller

import com.pruebas.PokerWebSocketHandler
import com.pruebas.dto.InsertTableDTO
import com.pruebas.error.exceptions.UnauthorizedException
import com.pruebas.model.Table
import com.pruebas.service.BetService
import com.pruebas.service.TableService
import com.pruebas.service.UsuarioService
import org.apache.coyote.BadRequestException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/tables")
class TableController(
    private val games: MutableMap<String, PokerWebSocketHandler>
) {

    @Autowired
    private lateinit var tableService: TableService

    @Autowired
    private lateinit var userService: UsuarioService

    @Autowired
    private lateinit var betsService: BetService

    @PostMapping("/insertTable")
    fun insertTable(
        authentication: Authentication,
        @RequestBody insertTableDto: InsertTableDTO): ResponseEntity<String> {

        val table = tableService.insertTable(insertTableDto)

        if (table._id == null) {
            throw BadRequestException("The id from the table is null")
        }

        val handler = PokerWebSocketHandler(
            table._id,
            table.bigBlind,
            userService,
            tableService,
            betsService)

        games[table._id] = handler

        return ResponseEntity.ok("Mesa '${table._id}' creada con big blind ${table.bigBlind}")
    }


//    @PostMapping("/insert")
//    fun insert(
//        authentication: Authentication,
//        @RequestBody tableDTO: InsertTableDTO,
//    ): ResponseEntity<Table> {
//        return ResponseEntity(tableService.insertTable(tableDTO) ,HttpStatus.CREATED)
//    }

    @GetMapping("/getAll")
    fun getTables():ResponseEntity<List<Table>> {
        return ResponseEntity(tableService.getAllTables(), HttpStatus.OK)
    }


    @PutMapping("/updateSum/{id}")
    fun updateSum(
        authentication: Authentication,
        @PathVariable id: String
    ): ResponseEntity<Table> {
        return ResponseEntity(tableService.sumOneNumOfPlayerFromTable(id),HttpStatus.OK)
    }

    @PutMapping("/updateSub/{id}")
    fun updateSub(
        authentication: Authentication,
        @PathVariable id: String
    ): ResponseEntity<Table> {
        return ResponseEntity(tableService.subOneNumOfPlayerFromTable(id),HttpStatus.OK)
    }

    @DeleteMapping("/deleteUselessTables")
    fun delete(
        authentication: Authentication
    ): ResponseEntity<String>{
        val tablesToDel: MutableList<Table> = mutableListOf()
        val alltables = tableService.getAllTables()

        for (table in alltables){
            if (table._id !in games.keys) {
                tablesToDel.add(table)
            }
        }

        tablesToDel.addAll(alltables.filter {
            it.numPlayers <= 0 && it !in tablesToDel
        })


        if (authentication.authorities.any { it.authority == "ROLE_ADMIN" }){
            tableService.deleteTables(tablesToDel)
            for (table in tablesToDel) {
                games.remove(table._id)
            }
            return ResponseEntity("All the useless tables were eliminated", HttpStatus.OK)
        }else{
            throw UnauthorizedException("You are not authorized to make this transaction")
        }
    }

}