package com.ntgjvmagent.authorizationserver.config

import com.ntgjvmagent.authorizationserver.repository.UserRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig(
    private val userRepository: UserRepository
) {

    @Bean
    @Order(2) // Run this after the AS chain
    fun defaultSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            // match everything ELSE, excluding SAS endpoints
            .securityMatcher("/**")
            .authorizeHttpRequests { auth ->
                auth.anyRequest().authenticated()
            }
            .formLogin { } // enables default login page
            .build()
    }

    @Bean
    fun userDetailsService(): UserDetailsService {
        return UserDetailsService { username ->
            val user = userRepository.findById(username)
                .orElseThrow { UsernameNotFoundException("User not found: $username") }

            User
                .withUsername(user.username)
                .password(user.password) // must already be encoded with BCrypt
                .roles(*user.roles.map { it.replace("ROLE_", "") }.toTypedArray())
                .disabled(!user.enabled)
                .build()
        }
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
}



