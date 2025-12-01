package com.ntgjvmagent.authorizationserver.service

import com.ntgjvmagent.authorizationserver.request.CreateUserRequest
import com.ntgjvmagent.authorizationserver.dto.CreateUserDto
import com.ntgjvmagent.authorizationserver.dto.UserDto
import com.ntgjvmagent.authorizationserver.dto.UserPageDto

interface UserService {

    fun getUsers(pageNumber: Int, pageSize: Int): UserPageDto

    fun createUser(request: CreateUserRequest): CreateUserDto

    fun deactivateUser(username: String): UserDto

    fun activateUser(username: String): UserDto
}
