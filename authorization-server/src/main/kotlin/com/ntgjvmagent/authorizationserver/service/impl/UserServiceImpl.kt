package com.ntgjvmagent.authorizationserver.service.impl

import com.ntgjvmagent.authorizationserver.dto.CreateUserDto
import com.ntgjvmagent.authorizationserver.dto.UserPageDto
import com.ntgjvmagent.authorizationserver.request.CreateUserRequest
import com.ntgjvmagent.authorizationserver.entity.UserRolesEntity
import com.ntgjvmagent.authorizationserver.mapper.toPageDto
import com.ntgjvmagent.authorizationserver.mapper.toUserEntity
import com.ntgjvmagent.authorizationserver.mapper.toCreateUserDto
import com.ntgjvmagent.authorizationserver.repository.UserRepository
import com.ntgjvmagent.authorizationserver.repository.RolesRepository
import com.ntgjvmagent.authorizationserver.service.UserService
import com.ntgjvmagent.authorizationserver.utils.PasswordGenerator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val rolesRepository: RolesRepository,
    private val passwordEncoder: PasswordEncoder
) : UserService {

    private val logger: Logger = LoggerFactory.getLogger(UserService::class.java)

    override fun getUsers(pageNumber: Int, pageSize: Int): UserPageDto {
        val pageable = PageRequest.of(pageNumber, pageSize)
        val page = userRepository.findAll(pageable)
        logger.debug("Retrieved {} users out of {} total", page.numberOfElements, page.totalElements)
        return page.toPageDto()
    }


    @Transactional
    override fun createUser(request: CreateUserRequest): CreateUserDto {
        // Validate username uniqueness
        require(userRepository.findUserByUserName(request.username).isEmpty) {
            "Username '${request.username}' already exists. Please use a different username."
        }

        val tempPassword = PasswordGenerator.generateTempPassword()
        val encodedPassword = passwordEncoder.encode(tempPassword)

        // Map request to entity
        val userEntity = request.toUserEntity(encodedPassword)

        // Add roles to the user
        request.roles.forEach { roleName ->
            val role = rolesRepository.findByName(roleName)
                .orElseThrow { IllegalArgumentException("Role '$roleName' not found") }
            userEntity.userRoles.add(UserRolesEntity(role = role, user = userEntity))
        }

        val savedUser = userRepository.save(userEntity)
        logger.info("User '{}' created successfully", request.username)
        return savedUser.toCreateUserDto(tempPassword)
    }
}
