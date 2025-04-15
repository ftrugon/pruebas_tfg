package com.pruebas.dto

class RegistrarUsuarioDTO (
    val username: String,
    val email: String,
    val password: String,
    val passwordRepeat: String,
    val rol: String?
)