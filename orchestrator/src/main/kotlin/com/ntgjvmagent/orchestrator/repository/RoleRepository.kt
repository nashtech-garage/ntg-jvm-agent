package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.RoleEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RoleRepository : JpaRepository<RoleEntity, UUID> {
    fun existsByName(name: String): Boolean

    fun findByName(name: String): RoleEntity?

    fun findByNameIn(names: List<String>): List<RoleEntity>
}
