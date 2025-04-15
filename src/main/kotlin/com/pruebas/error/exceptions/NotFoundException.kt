package com.pruebas.error.exceptions

class NotFoundException(message: String) : Exception("Not found exception (404). $message") {
}