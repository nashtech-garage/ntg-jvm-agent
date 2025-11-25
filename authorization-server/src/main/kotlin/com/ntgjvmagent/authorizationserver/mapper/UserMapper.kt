package com.ntgjvmagent.authorizationserver.mapper

import com.ntgjvmagent.authorizationserver.dto.UpdateUserResponseDto
import com.ntgjvmagent.authorizationserver.dto.UserDto
import com.ntgjvmagent.authorizationserver.dto.UserPageDto
import com.ntgjvmagent.authorizationserver.entity.UserEntity
import org.springframework.data.domain.Page

fun UserEntity.toDto() = UserDto(
    id = this.id,
    username = this.username,
    enabled = this.enabled,
    name = this.name,
    email = this.email,
    roles = this.userRoles.map { it.role.name }.toSet()
)

fun Page<UserEntity>.toPageDto() = UserPageDto(
    users = this.content.map { it.toDto() },
    pageNumber = this.number,
    pageSize = this.size,
    totalElements = this.totalElements,
    totalPages = this.totalPages,
    lastPage = this.isLast
)

fun UserEntity.toUpdateResponse(): UpdateUserResponseDto =
    UpdateUserResponseDto(
        id = id,
        username = username,
        name = name,
        email = email,
        enabled = enabled,
        roles = userRoles.map { it.role.name }
    )
