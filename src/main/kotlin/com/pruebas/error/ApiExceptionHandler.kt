package com.pruebas.error

import com.pruebas.error.exceptions.AlreadyExistException
import com.pruebas.error.exceptions.NotFoundException
import com.pruebas.error.exceptions.UnauthorizedException
import jakarta.servlet.http.HttpServletRequest
import org.apache.coyote.BadRequestException
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException
import javax.naming.AuthenticationException


@ControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(AlreadyExistException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    fun alreadyExistException(request: HttpServletRequest, e: Exception) : ErrorResponse {
        return ErrorResponse(e.message!!, request.requestURI)
    }

    @ExceptionHandler(IllegalArgumentException::class, BadRequestException::class,HttpMessageNotReadableException::class,
        MethodArgumentTypeMismatchException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    fun handleBadArguments(request: HttpServletRequest, e: Exception) : ErrorResponse {
        return ErrorResponse(e.message!!, request.requestURI)
    }

    @ExceptionHandler(AuthenticationException::class, UnauthorizedException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    fun handleAuthentication(request: HttpServletRequest, e: Exception) : ErrorResponse {
        return ErrorResponse(e.message!!, request.requestURI)
    }

    @ExceptionHandler(NotFoundException::class, NoResourceFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    fun notFound(request: HttpServletRequest, e: Exception) : ErrorResponse {
        return ErrorResponse(e.message!!, request.requestURI)
    }


    @ExceptionHandler(Exception::class, NullPointerException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    fun handleGeneric(request: HttpServletRequest, e: Exception) : ErrorResponse {
        return ErrorResponse(e.message!!, request.requestURI)
    }

}