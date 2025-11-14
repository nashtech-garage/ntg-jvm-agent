package com.ntgjvmagent.authorizationserver.service

import com.ntgjvmagent.authorizationserver.dto.UserPageDto

fun interface UserService {
    fun getUsers(pageNumber: Int, pageSize: Int): UserPageDto
}
