package com.ntgjvmagent.authorizationserver.repository

import com.ntgjvmagent.authorizationserver.entity.RolesEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface RolesRepository : JpaRepository<RolesEntity, UUID> {
    fun findByName(name: String): Optional<RolesEntity>
}


