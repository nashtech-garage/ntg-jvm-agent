package com.ntgjvmagent.authorizationserver.service

import com.ntgjvmagent.authorizationserver.dto.UpdateUserRequestDto
import com.ntgjvmagent.authorizationserver.dto.UpdateUserResponseDto
import com.ntgjvmagent.authorizationserver.request.CreateUserRequest
import com.ntgjvmagent.authorizationserver.dto.CreateUserDto
import com.ntgjvmagent.authorizationserver.dto.UserDto
import com.ntgjvmagent.authorizationserver.dto.UserPageDto
import java.util.UUID

interface UserService {
    fun getUsers(pageNumber: Int, pageSize: Int): UserPageDto

    fun updateUser(id: UUID, request: UpdateUserRequestDto): UpdateUserResponseDto

    fun createUser(request: CreateUserRequest): CreateUserDto

    fun deactivateUser(username: String): UserDto

    fun activateUser(username: String): UserDto
}
