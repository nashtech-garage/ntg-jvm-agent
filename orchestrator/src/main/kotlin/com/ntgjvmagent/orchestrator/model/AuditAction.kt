package com.ntgjvmagent.orchestrator.model

enum class AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    ACTIVATE,
    DEACTIVATE,
}

enum class ResourceType {
    USER,
    AGENT,
}
