package com.ntgjvmagent.authorizationserver.mapper

import com.ntgjvmagent.authorizationserver.dto.UserDto
import com.ntgjvmagent.authorizationserver.dto.UserPageDto
import com.ntgjvmagent.authorizationserver.entity.UserEntity
import org.springframework.data.domain.Page

fun UserEntity.toDto() = UserDto(
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
