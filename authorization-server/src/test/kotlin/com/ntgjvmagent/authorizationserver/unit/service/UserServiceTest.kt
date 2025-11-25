package com.ntgjvmagent.authorizationserver.unit.service

import com.ntgjvmagent.authorizationserver.dto.UpdateUserRequestDto
import com.ntgjvmagent.authorizationserver.entity.UserEntity
import com.ntgjvmagent.authorizationserver.exception.EmailAlreadyUsedException
import com.ntgjvmagent.authorizationserver.exception.UserNotFoundException
import com.ntgjvmagent.authorizationserver.exception.UsernameAlreadyUsedException
import com.ntgjvmagent.authorizationserver.repository.UserRepository
import com.ntgjvmagent.authorizationserver.service.impl.UserServiceImpl
import org.junit.Assert.assertThrows
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @InjectMocks
    private lateinit var userService: UserServiceImpl

    private val existingUser = UserEntity(
        id = UUID.randomUUID(),
        username = "olduser",
        password = "pass",
        enabled = true,
        name = "Old Name",
        email = "old@example.com"
    )

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
    fun `updateUser successfully`() {
        val request = UpdateUserRequestDto(
            username = "newuser",
            name = "New Name",
            email = "new@example.com"
        )

        val username = request.username!!
        val name = request.name!!
        val email = request.email!!

        `when`(userRepository.findById(existingUser.id)).thenReturn(Optional.of(existingUser))
        `when`(userRepository.findUserByUserName(username)).thenReturn(Optional.empty())
        `when`(userRepository.findByEmail(email)).thenReturn(Optional.empty())
        `when`(
            userRepository.save(
                existingUser.copy(
                    username = username,
                    name = name,
                    email = email
                )
            )
        ).thenAnswer { it.arguments[0] }

        val result = userService.updateUser(existingUser.id, request)

        assertEquals(username, result.username)
        assertEquals(name, result.name)
        assertEquals(email, result.email)

        verify(userRepository, times(1)).findById(existingUser.id)
        verify(userRepository, times(1)).findUserByUserName(username)
        verify(userRepository, times(1)).findByEmail(email)
        verify(userRepository, times(1)).save(
            existingUser.copy(
                username = username,
                name = name,
                email = email
            )
        )
    }

    @Test
    fun `updateUser throws UserNotFoundException if user not found`() {
        val request = UpdateUserRequestDto(username = "any")
        val id = UUID.randomUUID()

        `when`(userRepository.findById(id)).thenReturn(Optional.empty())

        assertThrows(UserNotFoundException::class.java) {
            userService.updateUser(id, request)
        }

        verify(userRepository, times(1)).findById(id)
    }

    @Test
    fun `updateUser throws UsernameAlreadyUsedException if username exists`() {
        val request = UpdateUserRequestDto(username = "existinguser")
        val username = request.username!!

        val otherUser = UserEntity(UUID.randomUUID(), "existinguser", "pass", true, "Name", "email@example.com")

        `when`(userRepository.findById(existingUser.id)).thenReturn(Optional.of(existingUser))
        `when`(userRepository.findUserByUserName(username)).thenReturn(Optional.of(otherUser))

        assertThrows(UsernameAlreadyUsedException::class.java) {
            userService.updateUser(existingUser.id, request)
        }

        verify(userRepository, times(1)).findById(existingUser.id)
        verify(userRepository, times(1)).findUserByUserName(username)
    }

    @Test
    fun `updateUser throws EmailAlreadyUsedException if email exists`() {
        val request = UpdateUserRequestDto(email = "existing@example.com")
        val email = request.email!!

        val otherUser = UserEntity(UUID.randomUUID(), "otheruser", "pass", true, "Name", "existing@example.com")

        `when`(userRepository.findById(existingUser.id)).thenReturn(Optional.of(existingUser))
        `when`(userRepository.findByEmail(email)).thenReturn(Optional.of(otherUser))

        assertThrows(EmailAlreadyUsedException::class.java) {
            userService.updateUser(existingUser.id, request)
        }

        verify(userRepository, times(1)).findById(existingUser.id)
        verify(userRepository, times(1)).findByEmail(email)
    }
}
