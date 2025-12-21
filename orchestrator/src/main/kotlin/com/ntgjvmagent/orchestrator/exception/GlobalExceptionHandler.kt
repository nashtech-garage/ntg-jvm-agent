package com.ntgjvmagent.orchestrator.exception

import jakarta.persistence.EntityNotFoundException
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    /* =========================================================
     * AUTHENTICATION
     * ========================================================= */

    @ExceptionHandler(UnauthenticatedUserException::class)
    fun handleUnauthenticated(
        ex: UnauthenticatedUserException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> {
        logger.warn(
            "Unauthenticated request: {} {} - {}",
            request.method,
            request.requestURI,
            ex.message,
        )

        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(
                ApiErrorResponse(
                    error = "UNAUTHENTICATED",
                    message = ex.message ?: "Authentication required",
                    path = request.requestURI,
                ),
            )
    }

    /* =========================================================
     * TOKEN / QUOTA
     * ========================================================= */

    @ExceptionHandler(TokenLimitExceededException::class)
    fun handleTokenLimitExceeded(
        ex: TokenLimitExceededException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> {
        logger.info(
            "Token limit exceeded for request {} {}: {}",
            request.method,
            request.requestURI,
            ex.message,
        )

        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .body(
                ApiErrorResponse(
                    error = "TOKEN_LIMIT_EXCEEDED",
                    message = ex.message ?: "Token quota exceeded",
                    path = request.requestURI,
                ),
            )
    }

    @ExceptionHandler(TokenQuotaUnavailableException::class)
    fun handleQuotaUnavailable(
        ex: TokenQuotaUnavailableException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> {
        logger.error(
            "Token quota service unavailable for {} {}",
            request.method,
            request.requestURI,
            ex,
        )

        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(
                ApiErrorResponse(
                    error = "TOKEN_QUOTA_UNAVAILABLE",
                    message = ex.message ?: "Token quota service unavailable",
                    path = request.requestURI,
                ),
            )
    }

    /* =========================================================
     * 404 — Resource / Entity not found
     * ========================================================= */

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFound(
        ex: ResourceNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> {
        logger.warn(
            "Resource not found: {} {} - {}",
            request.method,
            request.requestURI,
            ex.message,
        )

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                ApiErrorResponse(
                    error = "RESOURCE_NOT_FOUND",
                    message = ex.message ?: "Requested resource not found",
                    path = request.requestURI,
                ),
            )
    }

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFound(
        ex: EntityNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> {
        logger.warn(
            "Entity not found: {} {} - {}",
            request.method,
            request.requestURI,
            ex.message,
        )

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                ApiErrorResponse(
                    error = "ENTITY_NOT_FOUND",
                    message = ex.message ?: "Requested entity not found",
                    path = request.requestURI,
                ),
            )
    }

    /* =========================================================
     * 400 — Bad request / validation
     * ========================================================= */

    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequest(
        ex: BadRequestException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> {
        logger.info(
            "Bad request: {} {} - {}",
            request.method,
            request.requestURI,
            ex.message,
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ApiErrorResponse(
                    error = "BAD_REQUEST",
                    message = ex.message ?: "Invalid request",
                    path = request.requestURI,
                ),
            )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> {
        val fieldErrors =
            ex.bindingResult.fieldErrors.associate { error ->
                error.field to (error.defaultMessage ?: "Invalid value")
            }

        logger.info(
            "Validation failed: {} {} - {}",
            request.method,
            request.requestURI,
            fieldErrors,
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ApiErrorResponse(
                    error = "VALIDATION_FAILED",
                    message = "Request validation failed",
                    path = request.requestURI,
                    details = fieldErrors,
                ),
            )
    }

    /* =========================================================
     * FALLBACK (last line of defense)
     * ========================================================= */

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(
        ex: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> {
        logger.error(
            "Unhandled exception for {} {}",
            request.method,
            request.requestURI,
            ex,
        )

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ApiErrorResponse(
                    error = "INTERNAL_ERROR",
                    message = "Unexpected server error",
                    path = request.requestURI,
                ),
            )
    }
}
