package com.ntgjvmagent.authorizationserver.config

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import com.ntgjvmagent.authorizationserver.repository.UserRepository
import com.ntgjvmagent.authorizationserver.service.CustomUserDetailsService
import jakarta.persistence.EntityNotFoundException
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.core.io.ClassPathResource
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.util.matcher.RequestMatcher
import java.security.KeyStore
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.UUID

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val userRepository: UserRepository,
) {
    @Bean
    @Order(1) // Run this filter chain first
    fun authorizationServerSecurityFilterChain(
        http: HttpSecurity,
    ): SecurityFilterChain {
        val authorizationServerConfigurer = OAuth2AuthorizationServerConfigurer()
        val endpointsMatcher: RequestMatcher = authorizationServerConfigurer.endpointsMatcher
        // attach to SAS endpoints
        http.securityMatcher(endpointsMatcher)
            .authorizeHttpRequests {
                // Allow discovery endpoints
                it.requestMatchers(
                    "/.well-known/oauth-authorization-server",
                    "/.well-known/openid-configuration"
                ).permitAll()
                // Anything else requires authentication
                .anyRequest().authenticated()
            }
            // Disable CSRF for the AS endpoints (token, JWKs, etc.)
            .csrf { it.ignoringRequestMatchers(endpointsMatcher) }
            .exceptionHandling { exceptions ->
                exceptions.defaultAuthenticationEntryPointFor(
                    BearerTokenAuthenticationEntryPoint(),
                    RequestMatcher { req -> req.requestURI == "/userinfo" }
                )
                exceptions.defaultAuthenticationEntryPointFor(
                    LoginUrlAuthenticationEntryPoint("/login"),
                    endpointsMatcher
                )
            }
            .with(authorizationServerConfigurer) {
                it.oidc { }
            }
            .oauth2ResourceServer { it.jwt {} }

        return http.build()
    }

    @Bean
    @Order(2)
    fun defaultSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/login",
                        "/error",
                        "/favicon.ico"
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            .formLogin { }
            .build()
    }

    @Bean
    fun jwkSource(): JWKSource<SecurityContext> {
        val ks = KeyStore.getInstance("PKCS12")
        val resource = ClassPathResource("authserver.p12")
        ks.load(resource.inputStream, "changeit".toCharArray())

        val key = ks.getKey("authserver", "changeit".toCharArray()) as RSAPrivateKey
        val cert = ks.getCertificate("authserver") as java.security.cert.X509Certificate
        val publicKey = cert.publicKey as RSAPublicKey

        val rsaKey = RSAKey.Builder(publicKey)
            .privateKey(key)
            .keyID("authserver-key") // stable key id
            .build()

        return ImmutableJWKSet(JWKSet(rsaKey))
    }

    @Bean
    fun jwtDecoder(jwkSource: JWKSource<SecurityContext>): JwtDecoder {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource)
    }

    /**
     * Customizes JWT tokens to include user roles and authorities.
     * This ensures that JWT access tokens and ID tokens contain the user's roles
     * for proper authorization in resource servers and client applications.
     */
    @Bean
    fun jwtTokenCustomizer(): OAuth2TokenCustomizer<JwtEncodingContext> {
        return OAuth2TokenCustomizer { context ->

            val authentication: Authentication = context.getPrincipal()
            val userId = runCatching {
                UUID.fromString(authentication.name)
            }.getOrNull() ?: return@OAuth2TokenCustomizer

            val user = userRepository.findByIdOrNull(userId)
                ?: throw EntityNotFoundException("User not found: $userId")

            val roles = authentication.authorities
                .mapNotNull { it.authority }
                .filter { it.startsWith("ROLE_") }
                .map { it.removePrefix("ROLE_") }

            if (context.tokenType == OAuth2TokenType.ACCESS_TOKEN ||
                context.tokenType.value == OidcParameterNames.ID_TOKEN
            ) {
                context.claims.subject(user.id.toString())
                context.claims.claim("user_id", user.id.toString())
                context.claims.claim("name", user.name)
                context.claims.claim("roles", roles)
            }
        }
    }

    @Bean
    fun userDetailsService(): UserDetailsService {
        return CustomUserDetailsService(userRepository)
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

    @Bean
    fun customAuthProvider(
        userDetailsService: UserDetailsService,
        passwordEncoder: PasswordEncoder
    ): AuthenticationProvider {
        val delegate = DaoAuthenticationProvider(userDetailsService)
        delegate.setPasswordEncoder(passwordEncoder)

        return object : AuthenticationProvider {

            override fun authenticate(authentication: Authentication): Authentication {
                val result = delegate.authenticate(authentication)

                val userId =
                    (result.principal as? CustomUserDetails)
                        ?.userId
                        ?.toString()
                        ?: result.name

                return UsernamePasswordAuthenticationToken.authenticated(
                    userId,
                    null,
                    result.authorities
                )
            }

            override fun supports(authentication: Class<*>): Boolean {
                return UsernamePasswordAuthenticationToken::class.java
                    .isAssignableFrom(authentication)
            }
        }
    }
}
