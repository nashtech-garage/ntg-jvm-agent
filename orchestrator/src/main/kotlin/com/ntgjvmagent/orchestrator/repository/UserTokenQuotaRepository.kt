package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.UserTokenQuota
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserTokenQuotaRepository : JpaRepository<UserTokenQuota, UUID> {
    fun findByUserId(userId: UUID): UserTokenQuota?
}
