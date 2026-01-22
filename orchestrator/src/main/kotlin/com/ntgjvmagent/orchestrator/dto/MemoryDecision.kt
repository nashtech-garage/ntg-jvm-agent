package com.ntgjvmagent.orchestrator.dto

import com.ntgjvmagent.orchestrator.enum.MemoryKind

data class MemoryDecision(
    val embed: Boolean,
    val score: Double,
    val kind: MemoryKind,
    val reasons: List<String>,
)
