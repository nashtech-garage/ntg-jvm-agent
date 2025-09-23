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

            // only register if not exists
            val existing = (registeredClientRepository as? JdbcRegisteredClientRepository)
                ?.findByClientId(clientId)

            if (existing == null) {
                val registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId(clientId)
                    .clientSecret(passwordEncoder.encode("demo-secret"))
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .redirectUri("http://127.0.0.1:8081/login/oauth2/code/demo-client-oidc")
                    .scope(OidcScopes.OPENID)
                    .scope(OidcScopes.PROFILE)
                    .scope("chatbot.read")
                    .scope("chatbot.write")
                    .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
                    .build()

                registeredClientRepository.save(registeredClient)
                println("Registered demo client with clientId=demo-client / secret=demo-secret")
            } else {
                println("Client $clientId already exists, skipping")
            }
        }
    }
}

