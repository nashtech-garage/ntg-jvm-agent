package com.ntgjvmagent.orchestrator.entity

import com.ntgjvmagent.orchestrator.entity.base.UserAuditedEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "conversation")
data class Conversation(
    @Column(columnDefinition = "TEXT")
    var title: String,
    @Column(name = "session_id")
    var sessionId: UUID? = null,
    @Column(name = "is_active")
    var isActive: Boolean = true,
    @OneToMany(
        mappedBy = "conversation",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    val messages: MutableList<ChatMessage> = mutableListOf(),
) : UserAuditedEntity() {
    override fun toString(): String = "Conversation(id=$id, sessionId=$sessionId, isActive=$isActive)"
}
