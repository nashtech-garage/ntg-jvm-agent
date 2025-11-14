package com.ntgjvmagent.authorizationserver.service.impl

import com.ntgjvmagent.authorizationserver.dto.CreateUserDto
import com.ntgjvmagent.authorizationserver.dto.UserPageDto
import com.ntgjvmagent.authorizationserver.dto.request.CreateUserRequest
import com.ntgjvmagent.authorizationserver.enum.UserRoleEnum
import com.ntgjvmagent.authorizationserver.mapper.toPageDto
import com.ntgjvmagent.authorizationserver.mapper.toUserEntity
import com.ntgjvmagent.authorizationserver.mapper.toCreateUserDto
import com.ntgjvmagent.authorizationserver.repository.UserRepository
import com.ntgjvmagent.authorizationserver.service.UserService
import com.ntgjvmagent.authorizationserver.utils.PasswordGenerator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) : UserService {

    private val logger: Logger = LoggerFactory.getLogger(UserService::class.java)

    override fun getUsers(pageNumber: Int, pageSize: Int): UserPageDto {
        val pageable = PageRequest.of(pageNumber, pageSize)
        val page = userRepository.findAll(pageable)
        logger.debug("Retrieved {} users out of {} total", page.numberOfElements, page.totalElements)
        return page.toPageDto()
    }


    override fun createUser(request: CreateUserRequest): CreateUserDto {
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
