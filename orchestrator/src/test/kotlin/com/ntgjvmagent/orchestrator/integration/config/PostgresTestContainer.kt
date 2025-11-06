package com.ntgjvmagent.orchestrator.integration.config

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

class PostgresTestContainer {
    companion object {
        val container =
            PostgreSQLContainer(DockerImageName.parse("pgvector/pgvector:pg17"))
                .apply {
                    withDatabaseName("testdb")
                    withUsername("test")
                    withPassword("test")
                    start()
                }
    }

    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(context: ConfigurableApplicationContext) {
            TestPropertyValues
                .of(
                    "spring.datasource.url=${container.jdbcUrl}",
                    "spring.datasource.username=${container.username}",
                    "spring.datasource.password=${container.password}",
                ).applyTo(context.environment)
        }
    }
}
