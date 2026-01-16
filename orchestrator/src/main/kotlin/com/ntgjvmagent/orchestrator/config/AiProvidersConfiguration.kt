package com.ntgjvmagent.orchestrator.config

import com.azure.ai.openai.OpenAIClientBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration to provide Azure OpenAI client builder dynamically.
 * We use dynamic provider creation instead of Spring Boot auto-configuration.
 */
@Configuration
class AiProvidersConfiguration {
    /**
     * Provide a placeholder OpenAIClientBuilder to prevent auto-configuration errors.
     * The actual client creation is handled directly by ChatModelProvider.
     */
    @Bean
    @ConditionalOnMissingBean(name = ["openAIClientBuilder"])
    fun openAIClientBuilder(): OpenAIClientBuilder {
        // Return a builder with placeholder endpoint
        // This prevents the auto-configuration from failing with "Endpoint must not be empty"
        return OpenAIClientBuilder()
            .endpoint("https://placeholder.openai.azure.com/")
    }
}
