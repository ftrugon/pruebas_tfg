package com.pruebas.error.exceptions

class AlreadyExistException(message: String) : Exception("Conflict exception (409).$message") {
}