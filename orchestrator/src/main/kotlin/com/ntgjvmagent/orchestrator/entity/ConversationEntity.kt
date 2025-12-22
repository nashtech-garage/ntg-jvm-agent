package com.ntgjvmagent.orchestrator.entity

import com.ntgjvmagent.orchestrator.entity.base.UserAuditedEntity
import com.ntgjvmagent.orchestrator.model.ConversationStatus
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "conversation")
data class ConversationEntity(
    @Column(columnDefinition = "TEXT")
    var title: String,
    @Enumerated(EnumType.STRING)
    var status: ConversationStatus = ConversationStatus.DRAFT,
    @Column(name = "is_active")
    var isActive: Boolean = true,
    @OneToMany(
        mappedBy = "conversation",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    val messages: MutableList<ChatMessageEntity> = mutableListOf(),
) : UserAuditedEntity()
