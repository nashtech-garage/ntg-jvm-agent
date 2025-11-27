package com.ntgjvmagent.orchestrator.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain

@Configuration
class JwtAuthConverterConfig(
    private val env: Environment,
) {
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
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun prometheusUserDetailsService(passwordEncoder: PasswordEncoder): UserDetailsService {
        val username = env.getProperty("security.prometheus.username")
        val password = env.getProperty("security.prometheus.password")
        val role = env.getProperty("security.prometheus.role")
        val user =
            User
                .builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .roles(role)
                .build()
        return InMemoryUserDetailsManager(user)
    }

    /**
     * Configures a security filter chain specifically for the Prometheus actuator endpoint.
     * Uses HTTP Basic authentication with an in-memory user to secure metrics scraping.
     * This filter chain has @Order(1) to ensure it's evaluated before the main OAuth2 filter chain.
     *
     * @param http the HttpSecurity to configure
     * @param prometheusUserDetailsService the UserDetailsService containing Prometheus credentials
     * @return the configured SecurityFilterChain for the Prometheus endpoint
     */
    @Bean
    @Order(1)
    fun prometheusSecurityFilterChain(
        http: HttpSecurity,
        prometheusUserDetailsService: UserDetailsService,
    ): SecurityFilterChain {
        http
            .securityMatcher("/actuator/prometheus")
            .csrf { it.disable() }
            .userDetailsService(prometheusUserDetailsService)
            .authorizeHttpRequests {
                it.anyRequest().hasRole("PROMETHEUS")
            }.httpBasic(Customizer.withDefaults())
            .oauth2ResourceServer { it.disable() }
        return http.build()
    }

    @Bean
    @Order(2)
    fun securityFilterChain(
        http: HttpSecurity,
        jwtAuthConverter: JwtAuthenticationConverter,
    ): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers("/public/**").permitAll()
                it.requestMatchers(HttpMethod.GET, "/api/share/shared-conversations/{shareToken}").permitAll()
                it.anyRequest().authenticated()
            }.oauth2ResourceServer { rs ->
                rs.jwt { jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter) }
            }
        return http.build()
    }
}
