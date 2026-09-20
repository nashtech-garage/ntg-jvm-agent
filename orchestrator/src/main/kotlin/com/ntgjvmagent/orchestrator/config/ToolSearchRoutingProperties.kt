package com.ntgjvmagent.orchestrator.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tool-search.routing")
data class ToolSearchRoutingProperties(
    val minCatalogSize: Int = DEFAULT_MIN_CATALOG_SIZE,
) {
    init {
        require(minCatalogSize > 0) { "minCatalogSize must be greater than 0" }
    }

    fun shouldUseToolSearch(toolCount: Int): Boolean {
        require(toolCount >= 0) { "toolCount must not be negative" }
        return toolCount >= minCatalogSize
    }

    companion object {
        const val DEFAULT_MIN_CATALOG_SIZE = 10
    }
}
