package com.ntgjvmagent.authorizationserver.service.impl

import com.ntgjvmagent.authorizationserver.dto.CreateUserDto
import com.ntgjvmagent.authorizationserver.dto.UserDto
import com.ntgjvmagent.authorizationserver.dto.UserPageDto
import com.ntgjvmagent.authorizationserver.entity.UserEntity
import com.ntgjvmagent.authorizationserver.entity.UserRolesEntity
import com.ntgjvmagent.authorizationserver.mapper.toCreateUserDto
import com.ntgjvmagent.authorizationserver.mapper.toDto
import com.ntgjvmagent.authorizationserver.mapper.toPageDto
import com.ntgjvmagent.authorizationserver.mapper.toUserEntity
import com.ntgjvmagent.authorizationserver.repository.RolesRepository
import com.ntgjvmagent.authorizationserver.repository.UserRepository
import com.ntgjvmagent.authorizationserver.request.CreateUserRequest
import com.ntgjvmagent.authorizationserver.service.UserService
import com.ntgjvmagent.authorizationserver.utils.PasswordGenerator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import java.time.OffsetDateTime

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val rolesRepository: RolesRepository,
    private val passwordEncoder: PasswordEncoder
) : UserService {

    private val logger: Logger = LoggerFactory.getLogger(UserService::class.java)

    override fun getUsers(pageNumber: Int, pageSize: Int, currentUserId: UUID): UserPageDto {
        val pageable = PageRequest.of(pageNumber, pageSize)
        val page = userRepository.findAllExcept(currentUserId, pageable)
        logger.debug("Retrieved {} users out of {} total", page.numberOfElements, page.totalElements)
        return page.toPageDto()
    }


    @Transactional
    override fun createUser(request: CreateUserRequest): CreateUserDto {
        // Validate username uniqueness
        require(!userRepository.findUserByUserName(request.username).isPresent) {
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

    override fun deactivateUser(username: String): UserDto {
        val user = getUserByUserName(username)
        val updatedUser = user.copy(enabled = false)
        val saved = userRepository.save(updatedUser)
        logger.info("User '{}' activated successfully", username)

        return saved.toDto()
    }

    override fun activateUser(username: String): UserDto {
        val user = getUserByUserName(username)
        val updatedUser = user.copy(enabled = true)
        val saved = userRepository.save(updatedUser)
        logger.info("User '{}' deactivated successfully", username)
        return saved.toDto()
    }

    @Transactional
    override fun deleteUser(username: String) {
        val user = getUserByUserName(username)
        val deletedUser = user.copy(
            enabled = false,
            deletedAt = OffsetDateTime.now()
        )
        userRepository.save(deletedUser)
        userRepository.delete(deletedUser)
        logger.info("User '{}' deleted successfully", username)
    }

    fun getUserByUserName(username: String): UserEntity {
        return userRepository.findUserByUserName(username)
            .orElseThrow { IllegalArgumentException("User '$username' not found") }
    }
}
