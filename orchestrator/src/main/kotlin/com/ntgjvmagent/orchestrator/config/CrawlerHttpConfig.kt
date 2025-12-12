package com.ntgjvmagent.orchestrator.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration
class CrawlerHttpConfig {
    @Bean
    @Qualifier("crawlerRestClient")
    fun crawlerRestClient(builder: RestClient.Builder): RestClient {
        val factory = HttpComponentsClientHttpRequestFactory()

        return builder
            .requestFactory(factory)
            .build()
    }
}
