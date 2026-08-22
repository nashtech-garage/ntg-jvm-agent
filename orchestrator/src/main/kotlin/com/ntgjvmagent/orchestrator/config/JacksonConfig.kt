package com.ntgjvmagent.orchestrator.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper

@Configuration
class JacksonConfig {
    @Bean
    fun objectMapper(): ObjectMapper =
        JsonMapper
            .builder()
            .findAndAddModules()
            .build()
}
