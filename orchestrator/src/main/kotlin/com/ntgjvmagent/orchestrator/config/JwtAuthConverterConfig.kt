package com.ntgjvmagent.orchestrator.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain

@Configuration
class JwtAuthConverterConfig {
    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val converter = JwtAuthenticationConverter()
        converter.setJwtGrantedAuthoritiesConverter { jwt ->
            val authorities = mutableListOf<GrantedAuthority>()
            val scpScopes =
                when (val scpClaim = jwt.claims["scope"]) {
                    is String -> scpClaim.split(" ").filter(String::isNotBlank)
                    is Collection<*> -> scpClaim.mapNotNull { it?.toString() }
                    else -> emptyList()
                }
            authorities.addAll(scpScopes.map { SimpleGrantedAuthority("SCOPE_$it") })
            val roleClaims =
                when (val roles = jwt.claims["roles"]) {
                    is String -> roles.split(",").map { it.trim() }.filter(String::isNotBlank)
                    is Collection<*> -> roles.mapNotNull { it?.toString() }
                    else -> emptyList()
                }
            authorities.addAll(roleClaims.map { SimpleGrantedAuthority("ROLE_$it") })
            // Return authorities
            authorities
        }
        return converter
    }

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        jwtAuthConverter: JwtAuthenticationConverter,
    ): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers("/public/**").permitAll()
                it.requestMatchers("/api/knowledge").hasRole("ADMIN")
                it.requestMatchers("/api/roles").hasRole("ADMIN")
                it
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/**",
                    ).hasAnyAuthority("SCOPE_chatbot.read", "SCOPE_chatbot.write")
                listOf(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE).forEach { method ->
                    it.requestMatchers(method, "/api/**").hasAuthority("SCOPE_chatbot.write")
                }
                it.anyRequest().authenticated()
            }.oauth2ResourceServer { rs ->
                rs.jwt { jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter) }
            }
        return http.build()
    }
}
