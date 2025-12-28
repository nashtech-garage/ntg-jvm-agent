package com.ntgjvmagent.orchestrator.entity

import com.ntgjvmagent.orchestrator.model.TokenOperation
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "token_usage_daily")
class TokenUsageDaily {
    @Id
    @GeneratedValue
    var id: Long? = null

    lateinit var usageDate: LocalDate
    lateinit var agentId: UUID
    var userId: UUID? = null

    lateinit var provider: String
    lateinit var model: String

    @Enumerated(EnumType.STRING)
    lateinit var operation: TokenOperation

    var promptTokens: Long = 0
    var completionTokens: Long = 0
    var totalTokens: Long = 0
}
