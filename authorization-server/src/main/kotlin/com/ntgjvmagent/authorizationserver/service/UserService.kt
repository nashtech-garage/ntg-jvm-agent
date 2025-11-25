package com.ntgjvmagent.authorizationserver.service

import com.ntgjvmagent.authorizationserver.dto.UpdateUserRequestDto
import com.ntgjvmagent.authorizationserver.dto.UpdateUserResponseDto
import com.ntgjvmagent.authorizationserver.dto.UserPageDto
import java.util.UUID

interface UserService {
    fun getUsers(pageNumber: Int, pageSize: Int): UserPageDto

    fun updateUser(id: UUID, request: UpdateUserRequestDto): UpdateUserResponseDto
}
