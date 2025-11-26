package com.ntgjvmagent.authorizationserver.service.impl

import com.ntgjvmagent.authorizationserver.dto.UpdateUserRequestDto
import com.ntgjvmagent.authorizationserver.dto.UpdateUserResponseDto
import com.ntgjvmagent.authorizationserver.dto.UserPageDto
import com.ntgjvmagent.authorizationserver.exception.EmailAlreadyUsedException
import com.ntgjvmagent.authorizationserver.exception.UserNotFoundException
import com.ntgjvmagent.authorizationserver.exception.UsernameAlreadyUsedException
import com.ntgjvmagent.authorizationserver.mapper.toPageDto
import com.ntgjvmagent.authorizationserver.mapper.toUpdateResponse
import com.ntgjvmagent.authorizationserver.dto.UpdateUserRequestDto
import com.ntgjvmagent.authorizationserver.dto.UpdateUserResponseDto
import com.ntgjvmagent.authorizationserver.dto.CreateUserDto
import com.ntgjvmagent.authorizationserver.dto.UserPageDto
import com.ntgjvmagent.authorizationserver.exception.EmailAlreadyUsedException
import com.ntgjvmagent.authorizationserver.exception.UserNotFoundException
import com.ntgjvmagent.authorizationserver.exception.UsernameAlreadyUsedException
import com.ntgjvmagent.authorizationserver.request.CreateUserRequest
import com.ntgjvmagent.authorizationserver.entity.UserRolesEntity
import com.ntgjvmagent.authorizationserver.mapper.toPageDto
import com.ntgjvmagent.authorizationserver.mapper.toUpdateResponse
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
import java.util.UUID

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
    override fun updateUser(id: UUID, request: UpdateUserRequestDto): UpdateUserResponseDto {
        val user = userRepository.findById(id).orElseThrow {
            UserNotFoundException("User with id '$id' not found")
        }

        // Username unique check
        request.username?.let { newUsername ->
            if (newUsername != user.username) {
                val existed = userRepository.findUserByUserName(newUsername)
                if (existed.isPresent) {
                    throw UsernameAlreadyUsedException("Username '$newUsername' is already used")
                }
            }
        }

        // Email unique check
        request.email?.let { newEmail ->
            if (newEmail != user.email) {
                val existed = userRepository.findByEmail(newEmail)
                if (existed.isPresent) {
                    throw EmailAlreadyUsedException("Email '$newEmail' is already used")
                }
            }
        }

        val updated = user.copy(
            username = request.username ?: user.username,
            name = request.name ?: user.name,
            email = request.email ?: user.email,
        )

        return userRepository.save(updated).toUpdateResponse()
    @Transactional
    override fun updateUser(id: UUID, request: UpdateUserRequestDto): UpdateUserResponseDto {
        val user = userRepository.findById(id).orElseThrow {
            UserNotFoundException("User with id '$id' not found")
        }

        // Username unique check
        request.username?.let { newUsername ->
            if (newUsername != user.username) {
                val existed = userRepository.findUserByUserName(newUsername)
                if (existed.isPresent) {
                    throw UsernameAlreadyUsedException("Username '$newUsername' is already used")
                }
            }
        }

        // Email unique check
        request.email?.let { newEmail ->
            if (newEmail != user.email) {
                val existed = userRepository.findByEmail(newEmail)
                if (existed.isPresent) {
                    throw EmailAlreadyUsedException("Email '$newEmail' is already used")
                }
            }
        }

        val updated = user.copy(
            username = request.username ?: user.username,
            name = request.name ?: user.name,
            email = request.email ?: user.email,
        )

        return userRepository.save(updated).toUpdateResponse()
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
}
