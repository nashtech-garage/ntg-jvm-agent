package com.ntgjvmagent.authorizationserver.config

import jakarta.servlet.SessionTrackingMode
import org.springframework.boot.web.servlet.ServletContextInitializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ServletConfig {
    @Bean
    fun disableUrlSessionIds(): ServletContextInitializer {
        return ServletContextInitializer { servletContext ->
            servletContext.setSessionTrackingModes(setOf(SessionTrackingMode.COOKIE))
        }
    }
}

