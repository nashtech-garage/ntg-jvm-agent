package com.ntgjvmagent.orchestrator.model

import com.ntgjvmagent.orchestrator.exception.BadRequestException

enum class MemoryType(
    val value: String,
) {
    USER("user"),
    FEEDBACK("feedback"),
    PROJECT("project"),
    REFERENCE("reference"),
    ;

    companion object {
        fun fromValue(value: String): MemoryType =
            entries.firstOrNull { it.value == value.lowercase() }
                ?: throw BadRequestException(
                    "Memory type must be one of: ${entries.joinToString { it.value }}",
                )
    }
}
