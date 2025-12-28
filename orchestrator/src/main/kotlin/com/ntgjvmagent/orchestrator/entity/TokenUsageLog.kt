package com.ntgjvmagent.orchestrator.entity

import com.ntgjvmagent.orchestrator.model.TokenOperation
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "token_usage_log")
@Suppress("LongParameterList")
class TokenUsageLog(
    @Id
    val id: UUID = UUID.randomUUID(),
    val userId: UUID?,
    val agentId: UUID?,
    val organizationId: UUID?,
    val provider: String,
    val model: String,
    @Enumerated(EnumType.STRING)
    val operation: TokenOperation,
    val toolName: String?,
    val promptTokens: Long,
    val completionTokens: Long,
    val totalTokens: Long,
    val correlationId: String?,
    val createdAt: Instant = Instant.now(),
    /**
     * Derived by DB:
     * (created_at AT TIME ZONE 'UTC')::DATE
     */
    @Column(
        name = "usage_date",
        nullable = false,
        insertable = false,
        updatable = false,
    )
    val usageDate: LocalDate? = null,
)
