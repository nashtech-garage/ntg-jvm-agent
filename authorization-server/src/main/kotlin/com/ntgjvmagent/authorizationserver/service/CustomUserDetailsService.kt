package com.ntgjvmagent.authorizationserver.service

import com.ntgjvmagent.authorizationserver.config.CustomUserDetails
import com.ntgjvmagent.authorizationserver.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findUserByUserName(username)
            .orElseThrow { UsernameNotFoundException("User not found: $username") }

        val authorities = user.userRoles.map { SimpleGrantedAuthority(it.role.name) }

        return CustomUserDetails(
            userId = user.id!!,
            username = user.username,
            password = user.password,
            authorities = authorities,
        )
    }
}

