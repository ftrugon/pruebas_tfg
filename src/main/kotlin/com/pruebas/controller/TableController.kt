package com.pruebas.controller

import com.pruebas.dto.InsertTableDTO
import com.pruebas.model.Table
import com.pruebas.service.TableService
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
class TableController {

    @Autowired
    private lateinit var tableService: TableService


    @PostMapping("/insert")
    fun insert(
        authentication: Authentication,
        @RequestBody tableDTO: InsertTableDTO,
    ): ResponseEntity<Table> {
        return ResponseEntity(tableService.createTable(tableDTO) ,HttpStatus.CREATED)
    }

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

    @DeleteMapping("/deleteEmptyTables")
    fun delete(
        authentication: Authentication
    ){

        if (authentication.authorities.any { it.authority == "ROLE_ADMIN" }){
            tableService.deleteEmptyTables()
        }
    }


}