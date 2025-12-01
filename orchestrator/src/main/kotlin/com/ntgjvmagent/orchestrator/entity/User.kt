package com.ntgjvmagent.orchestrator.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "users")
data class User(
    @Id
    val id: UUID,
    val username: String,
    val password: String,
    val enabled: Boolean = true,
    val name: String,
    val email: String,
)
