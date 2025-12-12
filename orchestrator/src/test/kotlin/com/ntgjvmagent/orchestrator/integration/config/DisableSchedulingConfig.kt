package com.ntgjvmagent.orchestrator.integration.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.SchedulingConfigurer
import java.util.concurrent.Executors

@TestConfiguration
class DisableSchedulingConfig {
    @Bean
    fun stopScheduling() =
        SchedulingConfigurer { taskRegistrar ->
            taskRegistrar.setScheduler(
                Executors.newSingleThreadScheduledExecutor { r ->
                    Thread(r).apply { isDaemon = true } // daemon thread → JVM won't block
                },
            )
        }
}
