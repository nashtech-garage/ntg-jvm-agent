package com.ntgjvmagent.orchestrator.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.retry.RetryPolicy
import org.springframework.core.retry.RetryTemplate

@Configuration
class RetryConfig {
    @Bean
    fun noRetryTemplate(): RetryTemplate = RetryTemplate(RetryPolicy.withMaxRetries(0))
}
