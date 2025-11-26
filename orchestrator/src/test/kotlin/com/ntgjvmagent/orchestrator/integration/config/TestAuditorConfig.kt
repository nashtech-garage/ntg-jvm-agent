package com.ntgjvmagent.orchestrator.integration.config

import com.ntgjvmagent.orchestrator.entity.User
import com.ntgjvmagent.orchestrator.repository.UserRepository
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.domain.AuditorAware
import java.util.UUID

@TestConfiguration
class TestAuditorConfig {
    @Bean
    fun auditorAware(userRepository: UserRepository): AuditorAware<User> =
        AuditorAware {
            // Return a fixed test user
            userRepository.findById(TEST_USER_ID)
        }

    companion object {
        val TEST_USER_ID: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")
    }
}
