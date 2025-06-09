package com.pruebas

import com.pruebas.dto.InsertTableDTO
import com.pruebas.dto.RegistrarUsuarioDTO
import com.pruebas.dto.UsuarioDTO
import com.pruebas.error.exceptions.AlreadyExistException
import com.pruebas.error.exceptions.NotFoundException
import com.pruebas.error.exceptions.UnauthorizedException
import com.pruebas.model.BetDocument
import com.pruebas.model.Table
import com.pruebas.model.Usuario
import com.pruebas.repository.BetRepository
import com.pruebas.repository.TableRepository
import com.pruebas.repository.UsuarioRepository
import com.pruebas.service.BetService
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
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import java.util.Optional
import kotlin.test.assertEquals
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder

@ExtendWith(MockitoExtension::class)
class BetServiceTest {


    @Mock
    private lateinit var betRepository: BetRepository

    @InjectMocks
    private lateinit var betService: BetService


    @Test
    fun `getBetsByUserId return list of bets for user`() {
        val userId = "user123"
        val bets = listOf(
            BetDocument("1", "Mesa A", "table1", userId, 100, "RAISE"),
            BetDocument("2", "Mesa B", "table2", userId, 50, "CALL")
        )

        `when`(betRepository.findAllByUserId(userId)).thenReturn(bets)

        val result = betService.getBetsByUserId(userId)

        assertEquals(2, result.size)
        assertEquals("RAISE", result[0].type)
        assertEquals("CALL", result[1].type)
    }


    @Test
    fun `insertBet save and return the bet`() {
        val bet = BetDocument("1", "Mesa A", "table1", "user123", 150, "ALL_IN")

        `when`(betRepository.save(bet)).thenReturn(bet)

        val result = betService.insertBet(bet)

        assertEquals("Mesa A", result.tableName)
        assertEquals(150, result.amount)
        assertEquals("ALL_IN", result.type)
    }

}
