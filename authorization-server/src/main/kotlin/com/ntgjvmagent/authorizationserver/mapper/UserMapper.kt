package com.ntgjvmagent.authorizationserver.mapper

import com.ntgjvmagent.authorizationserver.dto.CreateUserDto
import com.ntgjvmagent.authorizationserver.dto.UserDto
import com.ntgjvmagent.authorizationserver.dto.UserPageDto
import com.ntgjvmagent.authorizationserver.request.CreateUserRequest
import com.ntgjvmagent.authorizationserver.entity.UserEntity
import org.springframework.data.domain.Page

fun CreateUserRequest.toUserEntity(encodedPassword: String) = UserEntity(
    username = this.username,
    password = encodedPassword,
    enabled = true,
    name = this.name,
    email = this.email,
    roles = this.roles
)

fun UserEntity.toDto() = UserDto(
    username = this.username,
    enabled = this.enabled,
    name = this.name,
    email = this.email,
    roles = this.userRoles.map { it.role.name }.toSet()
)

fun UserEntity.toCreateUserDto(password: String) = CreateUserDto(
    username = this.username,
    name = this.name,
    email = this.email,
    roles = this.roles,
    temporaryPassword = password
)

fun Page<UserEntity>.toPageDto() = UserPageDto(
    users = this.content.map { it.toDto() },
    pageNumber = this.number,
    pageSize = this.size,
    totalElements = this.totalElements,
    totalPages = this.totalPages,
    lastPage = this.isLast
)
