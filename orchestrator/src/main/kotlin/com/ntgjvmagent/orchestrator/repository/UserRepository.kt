package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.Role
import com.ntgjvmagent.orchestrator.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findByUsername(username: String): User?
}
