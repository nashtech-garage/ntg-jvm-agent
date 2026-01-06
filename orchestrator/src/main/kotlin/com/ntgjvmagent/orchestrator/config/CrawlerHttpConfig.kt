package com.ntgjvmagent.orchestrator.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration
class CrawlerHttpConfig {
    @Bean
    fun crawlerRestClient(): RestClient =
        RestClient
            .builder()
            .requestFactory(HttpComponentsClientHttpRequestFactory())
            .build()
}
