package com.ntgjvmagent.orchestrator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableScheduling
@EnableAsync
@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = ["com.ntgjvmagent.orchestrator.config"])
class OrchestratorApplication

fun main(args: Array<String>) {
    runApplication<OrchestratorApplication>(*args)
}
