package com.ntgjvmagent.authorizationserver.dto

data class UserPageDto(
    val users: List<UserDto>,
    val pageNumber: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int,
    val lastPage: Boolean,
)
