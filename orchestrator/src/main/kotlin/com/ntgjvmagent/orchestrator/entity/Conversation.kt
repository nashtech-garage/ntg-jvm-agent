package com.ntgjvmagent.orchestrator.entity

import com.ntgjvmagent.orchestrator.entity.base.UserAuditedEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited

@Entity
@Table(name = "conversation")
data class Conversation(
@Audited
data class ConversationEntity(
    @Column(columnDefinition = "TEXT")
    var title: String,
    @Column(name = "is_active")
    var isActive: Boolean = true,
    @OneToMany(
        mappedBy = "conversation",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    val messages: MutableList<ChatMessage> = mutableListOf(),
    @NotAudited
    val messages: MutableList<ChatMessageEntity> = mutableListOf(),
) : UserAuditedEntity()
