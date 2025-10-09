package com.ntgjvmagent.authorizationserver.config

import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.oidc.OidcScopes
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import java.util.*

@Configuration
class RegisteredClientConfig {

    private val passwordEncoder: PasswordEncoder =
        PasswordEncoderFactories.createDelegatingPasswordEncoder()

    @Bean
    fun clientInitializer(registeredClientRepository: RegisteredClientRepository): CommandLineRunner {
        return CommandLineRunner {
            val clientId = "demo-client"

            // Check if client exists
            val existing = (registeredClientRepository as? JdbcRegisteredClientRepository)
                ?.findByClientId(clientId)

            // Create the correct client configuration
            val correctClient = RegisteredClient.withId(existing?.id ?: UUID.randomUUID().toString())
                .clientId(clientId)
                .clientSecret(passwordEncoder.encode("demo-secret"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://localhost:3000/auth/callback") // Chat UI
                .redirectUri("http://localhost:3001/auth/callback") // Admin UI
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("chatbot.read")
                .scope("chatbot.write")
                .scope("admin.read")
                .scope("admin.write")
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
                .build()

            if (existing == null) {
                // Register new client
                registeredClientRepository.save(correctClient)
                println("Registered new demo client with clientId=demo-client / secret=demo-secret")
            } else {
                // Update existing client to ensure correct configuration
                registeredClientRepository.save(correctClient)
                println("Updated existing demo client with correct redirect URIs")
            }
        }
    }
}
