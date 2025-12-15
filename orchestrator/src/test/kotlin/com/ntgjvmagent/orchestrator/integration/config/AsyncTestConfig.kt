package com.ntgjvmagent.orchestrator.integration.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@TestConfiguration
class AsyncTestConfig {
    @Bean
    @Primary
    fun taskExecutor(): Executor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = 1
            maxPoolSize = 1
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(5)
            initialize()
        }
}
