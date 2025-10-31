package com.ntgjvmagent.mcpserver.exception

import com.ntgjvmagent.mcpserver.viewmodel.ErrorResponseVm
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(ex: ResourceNotFoundException): ResponseEntity<ErrorResponseVm> {
        val error =
            ErrorResponseVm(
                status = HttpStatus.NOT_FOUND.value(),
                error = HttpStatus.NOT_FOUND.reasonPhrase,
                message = ex.message,
            )
        return ResponseEntity(error, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(BadRequestException::class)
    fun handleNotFound(ex: BadRequestException): ResponseEntity<ErrorResponseVm> {
        val error =
            ErrorResponseVm(
                status = HttpStatus.BAD_REQUEST.value(),
                error = HttpStatus.BAD_REQUEST.reasonPhrase,
                message = ex.message,
            )
        return ResponseEntity(error, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponseVm> {
        val errors = ex.bindingResult.fieldErrors.associate { it.field to it.defaultMessage }
        val error =
            ErrorResponseVm(
                status = HttpStatus.BAD_REQUEST.value(),
                error = HttpStatus.BAD_REQUEST.reasonPhrase,
                message = errors.values.toList().toString(),
            )
        return ResponseEntity(error, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneralExceptions(ex: Exception): ResponseEntity<ErrorResponseVm> {
        val error =
            ErrorResponseVm(
                status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                error = HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
                message = ex.message,
            )
        return ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
