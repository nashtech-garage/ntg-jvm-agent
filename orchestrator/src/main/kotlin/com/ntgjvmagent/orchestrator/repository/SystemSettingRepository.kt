package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.SystemSettingEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SystemSettingRepository : JpaRepository<SystemSettingEntity, UUID>
