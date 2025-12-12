package com.ntgjvmagent.orchestrator.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.support.RetryTemplate

@Configuration
class RetryConfig {
    @Bean
    fun noRetryTemplate(): RetryTemplate =
        RetryTemplate
            .builder()
            .maxAttempts(1) // no retry
            .fixedBackoff(1)
            .retryOn { false } // never retry
            .build()
}
