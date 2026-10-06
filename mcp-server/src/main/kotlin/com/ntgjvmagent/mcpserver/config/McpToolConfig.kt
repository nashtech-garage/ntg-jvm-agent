package com.ntgjvmagent.mcpserver.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class McpToolConfig {
    @Bean
    fun restTemplate(): RestTemplate = RestTemplate()
}
