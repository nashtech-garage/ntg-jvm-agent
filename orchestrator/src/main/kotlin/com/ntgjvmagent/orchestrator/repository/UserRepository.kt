package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<UserEntity, UUID> {
    fun findByUsername(username: String): UserEntity?
}
