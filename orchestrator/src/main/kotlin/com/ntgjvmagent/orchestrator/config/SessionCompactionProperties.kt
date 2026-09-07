package com.ntgjvmagent.orchestrator.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "session.compaction")
data class SessionCompactionProperties(
    val enabled: Boolean = true,
    val turnThreshold: Int = 12,
    val tokenThreshold: Int = 4_000,
    val maxEventsToKeep: Int = 6,
    val overlapSize: Int = 2,
) {
    fun resolve(settings: Map<String, Any>?): ResolvedSessionCompactionSettings {
        val overrides = settings?.get(SETTINGS_KEY) as? Map<*, *> ?: emptyMap<Any, Any>()
        return ResolvedSessionCompactionSettings(
            enabled = overrides.boolean("enabled") ?: enabled,
            turnThreshold = overrides.int("turnThreshold") ?: turnThreshold,
            tokenThreshold = overrides.int("tokenThreshold") ?: tokenThreshold,
            maxEventsToKeep = overrides.int("maxEventsToKeep") ?: maxEventsToKeep,
            overlapSize = overrides.int("overlapSize") ?: overlapSize,
        )
    }

    private fun Map<*, *>.boolean(key: String): Boolean? =
        when (val value = this[key]) {
            is Boolean -> value
            is String -> value.toBooleanStrictOrNull()
            else -> null
        }

    private fun Map<*, *>.int(key: String): Int? =
        when (val value = this[key]) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }

    companion object {
        const val SETTINGS_KEY = "sessionCompaction"
    }
}

data class ResolvedSessionCompactionSettings(
    val enabled: Boolean,
    val turnThreshold: Int,
    val tokenThreshold: Int,
    val maxEventsToKeep: Int,
    val overlapSize: Int,
) {
    init {
        require(turnThreshold > 0) { "turnThreshold must be greater than 0" }
        require(tokenThreshold > 0) { "tokenThreshold must be greater than 0" }
        require(maxEventsToKeep > 0) { "maxEventsToKeep must be greater than 0" }
        require(overlapSize >= 0) { "overlapSize must be at least 0" }
        require(overlapSize < maxEventsToKeep) {
            "overlapSize must be less than maxEventsToKeep"
        }
    }
}
