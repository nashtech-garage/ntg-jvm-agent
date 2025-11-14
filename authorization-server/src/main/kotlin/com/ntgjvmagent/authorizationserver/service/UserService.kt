package com.ntgjvmagent.authorizationserver.service

import com.ntgjvmagent.authorizationserver.dto.request.CreateUserRequest
import com.ntgjvmagent.authorizationserver.dto.CreateUserDto
import com.ntgjvmagent.authorizationserver.dto.UserPageDto
import com.ntgjvmagent.authorizationserver.enum.UserRoleEnum
import com.ntgjvmagent.authorizationserver.mapper.toPageDto
import com.ntgjvmagent.authorizationserver.mapper.toCreateUserDto
import com.ntgjvmagent.authorizationserver.mapper.toUserEntity
import com.ntgjvmagent.authorizationserver.repository.UserRepository
import com.ntgjvmagent.authorizationserver.utils.PasswordGenerator
import org.springframework.data.domain.PageRequest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

interface UserService {
    fun getUsers(pageNumber: Int, pageSize: Int): UserPageDto;
    fun createUser(request: CreateUserRequest) : CreateUserDto
}
