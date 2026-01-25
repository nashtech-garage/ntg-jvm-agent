package com.ntgjvmagent.orchestrator.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "vector.db.datasource")
data class VectorDbDataSourceProperties(
    val url: String,
    val username: String,
    val password: String,
)
