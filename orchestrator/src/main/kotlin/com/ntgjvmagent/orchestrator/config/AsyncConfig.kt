package com.ntgjvmagent.orchestrator.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfig {
    companion object {
        private const val CORE_POOL_SIZE = 4
        private const val MAX_POOL_SIZE = 10
        private const val THREAD_NAME_PREFIX = "async-worker-"
    }

    @Bean
    fun taskExecutor(): Executor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = CORE_POOL_SIZE
            maxPoolSize = MAX_POOL_SIZE
            setThreadNamePrefix(THREAD_NAME_PREFIX)
            initialize()
        }
}
