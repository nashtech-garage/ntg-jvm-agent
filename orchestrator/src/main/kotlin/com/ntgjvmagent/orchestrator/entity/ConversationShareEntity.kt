package com.ntgjvmagent.orchestrator.entity

import com.ntgjvmagent.orchestrator.entity.base.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.OffsetDateTime
import org.hibernate.annotations.Type
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType

@Entity
@Table(name = "conversation_share")
data class ConversationShareEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    val conversation: ConversationEntity,
    @Column(name = "shared_by_username", nullable = false)
    val sharedByUsername: String,
    @Column(name = "share_token", unique = true, nullable = false, length = 255)
    val shareToken: String,
    @Column(name = "is_expired")
    var isExpired: Boolean = false,
    @Column(name = "expires_at")
    val expiresAt: OffsetDateTime? = null,
    @Type(JsonBinaryType::class)
    @Column(name = "shared_message_ids", columnDefinition = "JSONB")
    val sharedMessageIds: List<String> = emptyList(), // JSONB array: ["uuid1", "uuid2", ...]
) : BaseEntity()

