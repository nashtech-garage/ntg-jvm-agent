package com.ntgjvmagent.orchestrator.component.audit

import com.ntgjvmagent.orchestrator.model.AuditAction
import com.ntgjvmagent.orchestrator.model.AuthUser
import com.ntgjvmagent.orchestrator.model.ResourceType
import java.util.UUID

class AuditEvent(
    val actor: AuthUser,
    val action: AuditAction,
    val resourceType: ResourceType,
    val resourceId: UUID?,
)
