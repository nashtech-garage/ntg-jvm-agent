package com.ntgjvmagent.authorizationserver.dto

data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String?
)
