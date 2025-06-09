package com.pruebas

import com.pruebas.dto.InsertTableDTO
import com.pruebas.dto.RegistrarUsuarioDTO
import com.pruebas.dto.UsuarioDTO
import com.pruebas.error.exceptions.AlreadyExistException
import com.pruebas.error.exceptions.NotFoundException
import com.pruebas.error.exceptions.UnauthorizedException
import com.pruebas.model.Table
import com.pruebas.model.Usuario
import com.pruebas.repository.TableRepository
import com.pruebas.repository.UsuarioRepository
import com.pruebas.service.TableService
import com.pruebas.service.UsuarioService
import com.pruebas.utils.DTOParser
import org.apache.coyote.BadRequestException
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import java.util.Optional
import kotlin.test.assertEquals
import org.mockito.Mockito.mockStatic
import org.springframework.security.crypto.password.PasswordEncoder

@ExtendWith(MockitoExtension::class)
class TableServiceTest {

    @Mock
    private lateinit var tableRepository: TableRepository

    @InjectMocks
    private lateinit var tableService: TableService

    // TESTS PARA INSERTAR MESA

    @Test
    fun `insertTable insert a valid table`() {
        val dto = InsertTableDTO("Mesa 1", "",10)
        val table = Table(null, "Mesa 1", "",0, 10)

        mockStatic(DTOParser::class.java).use {
            `when`(tableRepository.save(table)).thenReturn(table)

            val result = tableService.insertTable(dto)

            assertEquals("Mesa 1", result.title)
            assertEquals(10, result.bigBlind)
        }
    }

    @Test
    fun `insertTable throw when title is empty`() {
        val dto = InsertTableDTO("", "",10)

        val exception = assertThrows(BadRequestException::class.java) {
            tableService.insertTable(dto)
        }

        assertEquals("Title cannot be empty", exception.message)
    }

    @Test
    fun `insertTable throw when bigBlind is less than 2`() {
        val dto = InsertTableDTO("Mesa", "",1)

        val exception = assertThrows(BadRequestException::class.java) {
            tableService.insertTable(dto)
        }

        assertEquals("Big blind cannot be less than 2", exception.message)
    }

    // TESTS PARA OBTENER TODAS LAS MESAS

    @Test
    fun `getAllTables return all tables`() {
        val tables = listOf(
            Table("1", "Mesa 1","", 2, 10),
            Table("2", "Mesa 2", "",1, 20)
        )
        `when`(tableRepository.findAll()).thenReturn(tables)

        val result = tableService.getAllTables()

        assertEquals(2, result.size)
    }

    // TESTS PARA SUMAR 1 JUGADOR A UNA MESAA

    @Test
    fun `sumOneNumOfPlayerFromTable increment numPlayers`() {
        val table = Table("1", "Mesa 1", "",2, 10)
        `when`(tableRepository.findBy_id("1")).thenReturn(Optional.of(table))
        `when`(tableRepository.save(table)).thenReturn(table.apply { numPlayers = 2 })

        val result = tableService.sumOneNumOfPlayerFromTable("1")

        assertEquals(3, result.numPlayers)
    }

    @Test
    fun `sumOneNumOfPlayerFromTable throw if table not found`() {
        `when`(tableRepository.findBy_id("123")).thenReturn(Optional.empty())

       assertThrows(NotFoundException::class.java) {
            tableService.sumOneNumOfPlayerFromTable("123")
        }
    }

    @Test
    fun `sumOneNumOfPlayerFromTable throw if table is full`() {
        val table = Table("1", "Mesa llena", "",10, 6)
        `when`(tableRepository.findBy_id("1")).thenReturn(Optional.of(table))

        val exception = assertThrows(BadRequestException::class.java) {
            tableService.sumOneNumOfPlayerFromTable("1")
        }

        assertEquals("The table is already full", exception.message)
    }

    // TESTS PARA RESTAR 1 JUGADOR A LA MESA

    @Test
    fun `subOneNumOfPlayerFromTable decrement numPlayers`() {
        val table = Table("1", "Mesa 1", "",2, 10)
        `when`(tableRepository.findBy_id("1")).thenReturn(Optional.of(table))
        `when`(tableRepository.save(table)).thenReturn(table.apply { numPlayers = 2 })

        val result = tableService.subOneNumOfPlayerFromTable("1")

        assertEquals(1, result.numPlayers)
    }

    @Test
    fun `subOneNumOfPlayerFromTable throw if table not found`() {
        `when`(tableRepository.findBy_id("123")).thenReturn(Optional.empty())

        assertThrows(NotFoundException::class.java) {
            tableService.subOneNumOfPlayerFromTable("123")
        }

    }

    @Test
    fun `subOneNumOfPlayerFromTable throw if numPlayers is less than 0`() {
        val table = Table("1", "Mesa vacía", "",-1, 10)
        `when`(tableRepository.findBy_id("1")).thenReturn(Optional.of(table))

        val exception = assertThrows(BadRequestException::class.java) {
            tableService.subOneNumOfPlayerFromTable("1")
        }

        assertEquals("The table is already empty", exception.message)
    }

    // TESTS PARA ELIMINAR MESAS

    @Test
    fun `deleteTables call deleteById for each table`() {
        val tables = listOf(
            Table("1", "Mesa 1", "",10, 0),
            Table("2", "Mesa 2", "",20, 0)
        )

        tableService.deleteTables(tables)

        verify(tableRepository).deleteById("1")
        verify(tableRepository).deleteById("2")
    }
}
