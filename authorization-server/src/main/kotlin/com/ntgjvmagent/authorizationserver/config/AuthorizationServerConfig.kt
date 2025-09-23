package com.ntgjvmagent.authorizationserver.config

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.core.io.ClassPathResource
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.util.matcher.RequestMatcher
import java.security.KeyStore
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

@Configuration
@EnableWebSecurity
class AuthorizationServerConfig {

    @Bean
    @Order(1) // Run this filter chain first
    fun authorizationServerSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val authorizationServerConfigurer = OAuth2AuthorizationServerConfigurer()
        val endpointsMatcher: RequestMatcher = authorizationServerConfigurer.endpointsMatcher
        // attach to SAS endpoints
        http.securityMatcher(endpointsMatcher)
            .authorizeHttpRequests { auth ->
                auth
                    // Allow discovery endpoints
                    .requestMatchers(
                        "/.well-known/oauth-authorization-server",
                        "/.well-known/openid-configuration" // only if OIDC enabled
                    ).permitAll()
                    // Anything else requires authentication
                    .anyRequest().authenticated()
            }
            .csrf { csrf ->
                // Disable CSRF for the AS endpoints (token, JWKs, etc.)
                csrf.ignoringRequestMatchers(endpointsMatcher)
            }

        // New DSL instead of deprecated .apply()
        http.with(authorizationServerConfigurer) { }

        http.exceptionHandling { exceptions ->
            exceptions.defaultAuthenticationEntryPointFor(
                BearerTokenAuthenticationEntryPoint()
            ) { req -> req.requestURI == "/userinfo" }
            exceptions.defaultAuthenticationEntryPointFor(
                LoginUrlAuthenticationEntryPoint("/login")
            ) { true }
        }
        http.formLogin(Customizer.withDefaults())
        http.getConfigurer(OAuth2AuthorizationServerConfigurer::class.java)
            .oidc { oidc ->  // Enable OpenID Connect 1.0
                oidc.clientRegistrationEndpoint { }
            }
        http.oauth2ResourceServer { rs -> rs.jwt { } }

        return http.build()
    }

    @Bean
    fun authorizationServerSettings(): AuthorizationServerSettings =
        AuthorizationServerSettings.builder()
            .build()

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
}
