package com.ntgjvmagent.orchestrator.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcAsyncConfig : WebMvcConfigurer {
    companion object {
        private const val CORE_POOL_SIZE = 10
        private const val MAX_POOL_SIZE = 50
        private const val QUEUE_CAPACITY = 1_000
        private const val THREAD_NAME_PREFIX = "mvc-async-"
        private const val DEFAULT_TIMEOUT = 30_000L // 30s, tune as needed
    }

    override fun configureAsyncSupport(configurer: AsyncSupportConfigurer) {
        configurer.setTaskExecutor(mvcAsyncExecutor())
        configurer.setDefaultTimeout(DEFAULT_TIMEOUT)
    }

    @Bean
    fun mvcAsyncExecutor(): AsyncTaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = CORE_POOL_SIZE
        executor.maxPoolSize = MAX_POOL_SIZE
        executor.queueCapacity = QUEUE_CAPACITY
        executor.setThreadNamePrefix(THREAD_NAME_PREFIX)
        executor.initialize()
        return executor
    }
}
