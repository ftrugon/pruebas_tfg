package com.pruebas

import com.pruebas.dto.RegistrarUsuarioDTO
import com.pruebas.dto.UsuarioDTO
import com.pruebas.error.exceptions.AlreadyExistException
import com.pruebas.error.exceptions.NotFoundException
import com.pruebas.error.exceptions.UnauthorizedException
import com.pruebas.model.Usuario
import com.pruebas.repository.UsuarioRepository
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
class UserServiceTest {

    @Mock
    lateinit var usuarioRepository: UsuarioRepository


    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @InjectMocks
    private lateinit var usuarioService: UsuarioService

    // TESTS PARA LOGEARSE

    @Test
    fun `loadUserByUsername return UserDetails when user exists`() {
        val usuario = Usuario(
            _id = "1",
            username = "testUser",
            password = "encodedPass",
            tokens = 0,
            roles = "USER"
        )

        `when`(usuarioRepository.findByUsername("testUser"))
            .thenReturn(Optional.of(usuario))

        val result = usuarioService.loadUserByUsername("testUser")

        assertEquals("testUser", result.username)
        assertEquals("encodedPass", result.password)
        assertArrayEquals(arrayOf("ROLE_USER"), result.authorities.map { it.authority }.toTypedArray())
    }


    @Test
    fun `loadUserByUsername throws exception when user not found`() {
        `when`(usuarioRepository.findByUsername("noUser"))
            .thenReturn(Optional.empty())

        assertThrows(UnauthorizedException::class.java) {
            usuarioService.loadUserByUsername("noUser")
        }
    }



    // TESTS PARA INSERTAR USUARIO

    @Test
    fun `insertUser register user when data is valid`() {
        val dto = RegistrarUsuarioDTO(
            username = "newUser",
            password = "password123",
            passwordRepeat = "password123"
        )
        val usuario = Usuario(_id = null, username = "newUser", password = "", tokens = 0)
        val usuarioCodificado = usuario.copy(password = "encodedPassword")
        val usuarioDTO = UsuarioDTO("newUser", 0)

        mockStatic(DTOParser::class.java) { mockedParser ->
            `when`(usuarioRepository.findByUsername("newUser")).thenReturn(Optional.empty())
            `when`(passwordEncoder.encode("password123")).thenReturn("encodedPassword")
            `when`(usuarioRepository.save(usuarioCodificado)).thenReturn(usuarioCodificado)
            `when`(DTOParser.registrarDTOToUsuario(dto)).thenReturn(usuario)
            `when`(DTOParser.usuarioToDto(usuarioCodificado)).thenReturn(usuarioDTO)

            val result = usuarioService.insertUser(dto)

            assertEquals("newUser", result.username)
            assertEquals(0, result.tokens)
        }
    }

    @Test
    fun `insertUser throw when passwords do not match`() {
        val dto = RegistrarUsuarioDTO("user", "pass1", "pass2")

        assertThrows(BadRequestException::class.java) {
            usuarioService.insertUser(dto)
        }
    }

    @Test
    fun `insertUser throw when user already exists`() {
        val dto = RegistrarUsuarioDTO("existing", "pass123", "pass123")
        val existingUser = Usuario("id", "existing", "encoded", 0)

        `when`(usuarioRepository.findByUsername("existing")).thenReturn(Optional.of(existingUser))

        assertThrows(AlreadyExistException::class.java) {
            usuarioService.insertUser(dto)
        }
    }


    // TESTS PARA GET BY USERNAME
    @Test
    fun `getByUsername return user when exists`() {
        val user = Usuario("1", "user", "pass", 0)
        `when`(usuarioRepository.findByUsername("user")).thenReturn(Optional.of(user))

        val result = usuarioService.getByUsername("user")

        assertEquals(user, result)
    }

    @Test
    fun `getByUsername throw when user not found`() {
        `when`(usuarioRepository.findByUsername("user")).thenReturn(Optional.empty())

       assertThrows(NotFoundException::class.java) {
            usuarioService.getByUsername("user")
        }

    }

    // TESTS PARA AÑADIR TOKENS

    @Test
    fun `addTokensToUser add tokens and return updated DTO`() {
        val user = Usuario("1", "user", "pass", 10)

        `when`(usuarioRepository.findByUsername("user")).thenReturn(Optional.of(user))
        `when`(usuarioRepository.save(user)).thenReturn(user)

        mockStatic(DTOParser::class.java).use {

            val result = usuarioService.addTokensToUser("user", 10)

            assertEquals(20, result.tokens)
        }
    }

    // TESTS PARA RETIRAR TOKENS

    @Test
    fun `retireTokensToUser subtract tokens and return updated DTO`() {
        val user = Usuario("1", "user", "pass", 20)
        val updatedUser = user.copy(tokens = 10)

        `when`(usuarioRepository.findByUsername("user")).thenReturn(Optional.of(user))
        `when`(usuarioRepository.save(updatedUser)).thenReturn(updatedUser)

        mockStatic(DTOParser::class.java).use {

            val result = usuarioService.retireTokensToUser("user", 10)

            assertEquals(10, result.tokens)
        }
    }

    @Test
    fun `retireTokensToUser throw when amount exceeds balance`() {
        val user = Usuario("1", "user", "pass", 5)
        `when`(usuarioRepository.findByUsername("user")).thenReturn(Optional.of(user))

        assertThrows(BadRequestException::class.java) {
            usuarioService.retireTokensToUser("user", 10)
        }

    }

    // TESTS PARA BANEAR USUARIO

    @Test
    fun `banUser set isBanned true and save`() {
        val user = Usuario("1", "user", "pass", 0, isBanned = false)
        val bannedUser = user.copy(isBanned = true)

        `when`(usuarioRepository.findByUsername("user")).thenReturn(Optional.of(user))
        `when`(usuarioRepository.save(bannedUser)).thenReturn(bannedUser)

        val result = usuarioService.banUser("user")

        assertTrue(result.isBanned)
    }

    // TEST PARA CAMBIAR EL NMOMBRE A USUARIO

    @Test
    fun `changeUsername update username and return DTO`() {
        val usuarioOriginal = Usuario(
            _id = "",
            username = "Admin",
            password = "1234",
            tokens = 100,
            roles = "ADMIN"
        )

        val usuarioActualizado = usuarioOriginal.copy(username = "String")

        `when`(usuarioRepository.findByUsername("Admin"))
            .thenReturn(Optional.of(usuarioOriginal))

        `when`(usuarioRepository.save(any()))
            .thenReturn(usuarioActualizado)

        val resultado = usuarioService.changeUsername("Admin", "String")

        assertEquals("String", resultado.username)
        verify(usuarioRepository).findByUsername("Admin")
        verify(usuarioRepository).save(usuarioOriginal)
    }

}