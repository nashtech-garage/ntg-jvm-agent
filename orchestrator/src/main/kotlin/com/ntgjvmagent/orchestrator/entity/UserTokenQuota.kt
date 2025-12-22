package com.ntgjvmagent.orchestrator.entity

import com.ntgjvmagent.orchestrator.entity.base.UserAuditedEntity
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "user_token_quota")
class UserTokenQuota(
    val userId: UUID,
    val dailyLimit: Long,
) : UserAuditedEntity()
