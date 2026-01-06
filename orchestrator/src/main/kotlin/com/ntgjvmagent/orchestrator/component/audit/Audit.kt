package com.ntgjvmagent.orchestrator.component.audit

import com.ntgjvmagent.orchestrator.model.AuditAction
import com.ntgjvmagent.orchestrator.model.ResourceType

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Audit(
    val action: AuditAction,
    val resource: ResourceType,
)
