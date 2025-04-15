package com.pruebas.error.exceptions

class UnauthorizedException(message: String) : Exception("Not authorized exception (401). $message") {
}