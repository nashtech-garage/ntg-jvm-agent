package com.ntgjvmagent.orchestrator.entity

import com.ntgjvmagent.orchestrator.entity.base.BaseEntity
import com.ntgjvmagent.orchestrator.model.AuditAction
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "audit_log")
data class AuditLog(
    val actorId: UUID,
    val actorName: String?,
    @Enumerated(EnumType.STRING)
    val action: AuditAction,
    val resourceType: String,
    val resourceId: UUID?,
) : BaseEntity()
