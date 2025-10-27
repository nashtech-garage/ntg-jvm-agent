// src/main/kotlin/com/ntgjvmagent/orchestrator/repository/RoleRepository.kt
package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.Role
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RoleRepository : JpaRepository<Role, UUID> {
    fun existsByName(name: String): Boolean
    fun findByName(name: String): Role?
}
