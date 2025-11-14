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

fun interface UserService {
    fun getUsers(pageNumber: Int, pageSize: Int): UserPageDto
@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    fun getUsers(pageNumber: Int, pageSize: Int): UserPageDto {
        val pageable = PageRequest.of(pageNumber, pageSize)
        val page = userRepository.findAll(pageable)
        return page.toPageDto()
    }


    fun createUser(request: CreateUserRequest): CreateUserDto {
        val tempPassword = PasswordGenerator.generateTempPassword()
        val encodedPassword = passwordEncoder.encode(tempPassword)

        // Allow only specific roles (USER). Ignore any roles not in the allow-list.
        val allowedRoles = setOf(
            UserRoleEnum.ROLE_USER.roleName
        )

        val requestedRoles = request.roles
        val filteredRoles = requestedRoles.filter { it in allowedRoles }.toSet()
        val finalRoles = filteredRoles.ifEmpty { setOf(UserRoleEnum.ROLE_USER.roleName) }

        val requestWithAllowedRoles = request.copy(roles = finalRoles)
        val userEntity = requestWithAllowedRoles.toUserEntity(encodedPassword)

        val savedUser = userRepository.save(userEntity)
        return savedUser.toCreateUserDto(tempPassword)
    }
}
