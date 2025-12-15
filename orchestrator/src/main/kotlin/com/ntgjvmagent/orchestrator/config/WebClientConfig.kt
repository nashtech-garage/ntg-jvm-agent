package com.ntgjvmagent.orchestrator.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig {
    companion object {
        /** Maximum in-memory buffer allowed for crawler (4 MB). */
        private const val MAX_IN_MEMORY_BYTES = 4 * 1024 * 1024
    }

    @Bean("crawlerWebClient")
    fun crawlerWebClient(builder: WebClient.Builder): WebClient =
        builder
            .codecs { config ->
                config.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_BYTES)
            }.build()
}
