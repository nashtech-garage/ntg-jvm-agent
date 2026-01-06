package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.AuditLog
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AuditLogRepository : JpaRepository<AuditLog, UUID>
