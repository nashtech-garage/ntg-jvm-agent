package com.ntgjvmagent.orchestrator.entity

import com.ntgjvmagent.orchestrator.entity.base.UserAuditedEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "conversation_share")
data class ConversationShareEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    val conversation: ConversationEntity,
    @Column(name = "share_token", unique = true, nullable = false, length = 255)
    val shareToken: String,
    @Column(name = "is_expired")
    var isExpired: Boolean = false,
    @Column(name = "expires_at")
    val expiresAt: Instant? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shared_message_ids", columnDefinition = "JSONB")
    val sharedMessageIds: List<String> = emptyList(), // JSONB array: ["uuid1", "uuid2", ...]
) : UserAuditedEntity()
