package com.ntgjvmagent.orchestrator.integration.config

import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class DisableVectorStoreInitializerConfig {
    @Bean
    @Primary
    fun vectorStoreInitializer(): SmartInitializingSingleton =
        SmartInitializingSingleton {
            // NO-OP
        }
}
