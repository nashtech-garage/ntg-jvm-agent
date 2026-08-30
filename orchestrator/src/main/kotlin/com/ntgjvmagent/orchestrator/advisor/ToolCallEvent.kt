package com.ntgjvmagent.orchestrator.advisor

/** A tool lifecycle event observed inside the Spring AI tool-calling loop. */
data class ToolCallEvent(
    val id: String,
    val name: String,
    val phase: Phase,
) {
    enum class Phase {
        STARTED,
        COMPLETED,
    }
}
