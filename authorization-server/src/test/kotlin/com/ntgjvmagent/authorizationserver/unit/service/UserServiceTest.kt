package com.ntgjvmagent.authorizationserver.unit.service

import com.ntgjvmagent.authorizationserver.entity.UserEntity
import com.ntgjvmagent.authorizationserver.repository.UserRepository
import com.ntgjvmagent.authorizationserver.service.impl.UserServiceImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @InjectMocks
    private lateinit var userService: UserServiceImpl

    @Test
    fun `getUsers return paginated users`() {
        val users = listOf(
            UserEntity(UUID.randomUUID(),"testuser1","password1",true,"Test","testuser@gmail.com"),
            UserEntity(UUID.randomUUID(),"admin","adminpass",true,"Admin","admin@gmail.com")
        )

        val pageable = PageRequest.of(0, 2)
        val page: Page<UserEntity> = PageImpl(users, pageable, users.size.toLong())

        `when`(userRepository.findAll(pageable)).thenReturn(page)

        val result = userService.getUsers(0, 2)
        assertEquals(1, result.totalPages)
        assertEquals(2, result.users.size)
        verify(userRepository, times(1)).findAll(pageable)

    }
}
