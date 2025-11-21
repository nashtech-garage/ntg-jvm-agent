package com.ntgjvmagent.authorizationserver.unit.service

import com.ntgjvmagent.authorizationserver.entity.UserEntity
import com.ntgjvmagent.authorizationserver.entity.RolesEntity
import com.ntgjvmagent.authorizationserver.entity.UserRolesEntity
import com.ntgjvmagent.authorizationserver.enum.UserRoleEnum
import com.ntgjvmagent.authorizationserver.repository.UserRepository
import com.ntgjvmagent.authorizationserver.repository.RolesRepository
import com.ntgjvmagent.authorizationserver.request.CreateUserRequest
import com.ntgjvmagent.authorizationserver.service.impl.UserServiceImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.Optional
import java.util.UUID
import org.springframework.security.crypto.password.PasswordEncoder

@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var rolesRepository: RolesRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @InjectMocks
    private lateinit var userService: UserServiceImpl

    @Test
    fun `getUsers return paginated users`() {
        val users = listOf(
            UserEntity(UUID.randomUUID(), "testuser1", "password1", true, "Test", "testuser@gmail.com"),
            UserEntity(UUID.randomUUID(), "admin", "adminpass", true, "Admin", "admin@gmail.com")
        )

        val pageable = PageRequest.of(0, 2)
        val page: Page<UserEntity> = PageImpl(users, pageable, users.size.toLong())

        `when`(userRepository.findAll(pageable)).thenReturn(page)

        val result = userService.getUsers(0, 2)
        assertEquals(1, result.totalPages)
        assertEquals(2, result.users.size)
        verify(userRepository, times(1)).findAll(pageable)

    }

    @Test
    fun `createUser should create user with temporary password and ROLE_USER`() {
        val request = CreateUserRequest(
            username = "newuser",
            name = "New User",
            email = "newuser@gmail.com",
            sendAccountInfo = true
        )

        val encodedPassword = "encodedTempPassword123"
        val userId = UUID.randomUUID()
        val roleEntity = RolesEntity(UUID.randomUUID(), UserRoleEnum.ROLE_USER.roleName)

        val userEntity = UserEntity(
            id = userId,
            username = request.username,
            password = encodedPassword,
            enabled = true,
            name = request.name,
            email = request.email
        )

        // Add role to user entity
        userEntity.userRoles.add(UserRolesEntity(role = roleEntity, user = userEntity))

        `when`(userRepository.findUserByUserName(request.username)).thenReturn(Optional.empty())
        `when`(passwordEncoder.encode(any())).thenReturn(encodedPassword)
        `when`(rolesRepository.findByName(UserRoleEnum.ROLE_USER.roleName)).thenReturn(Optional.of(roleEntity))
        `when`(userRepository.save(any())).thenReturn(userEntity)

        val result = userService.createUser(request)

        assertEquals("newuser", result.username)
        assertEquals("New User", result.name)
        assertEquals("newuser@gmail.com", result.email)
        assertEquals(setOf(UserRoleEnum.ROLE_USER.roleName), result.roles)
        assertTrue(result.temporaryPassword.isNotBlank())
        verify(userRepository, times(1)).save(any())
        verify(passwordEncoder, times(1)).encode(any())
        verify(rolesRepository, times(1)).findByName(UserRoleEnum.ROLE_USER.roleName)
    }

    @Test
    fun `createUser should use default ROLE_USER when no role specified`() {
        val request = CreateUserRequest(
            username = "defaultroleuser",
            name = "Default Role User",
            email = "defaultrole@gmail.com",
            sendAccountInfo = true
        )

        val encodedPassword = "encodedTempPassword123"
        val userId = UUID.randomUUID()
        val roleEntity = RolesEntity(UUID.randomUUID(), UserRoleEnum.ROLE_USER.roleName)

        val userEntity = UserEntity(
            id = userId,
            username = request.username,
            password = encodedPassword,
            enabled = true,
            name = request.name,
            email = request.email
        )

        // Add role to user entity
        userEntity.userRoles.add(UserRolesEntity(role = roleEntity, user = userEntity))

        `when`(userRepository.findUserByUserName(request.username)).thenReturn(Optional.empty())
        `when`(passwordEncoder.encode(any())).thenReturn(encodedPassword)
        `when`(rolesRepository.findByName(UserRoleEnum.ROLE_USER.roleName)).thenReturn(Optional.of(roleEntity))
        `when`(userRepository.save(any())).thenReturn(userEntity)

        val result = userService.createUser(request)

        assertEquals("defaultroleuser", result.username)
        assertEquals(setOf(UserRoleEnum.ROLE_USER.roleName), result.roles)
        verify(userRepository, times(1)).save(any())
        verify(rolesRepository, times(1)).findByName(UserRoleEnum.ROLE_USER.roleName)
    }
}
