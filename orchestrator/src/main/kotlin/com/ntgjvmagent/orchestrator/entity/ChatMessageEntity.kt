package com.ntgjvmagent.orchestrator.entity

import com.ntgjvmagent.orchestrator.entity.base.UserAuditedEntity
import com.ntgjvmagent.orchestrator.entity.enums.MessageReaction
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "chat_message")
data class ChatMessageEntity(
    @Column(columnDefinition = "TEXT")
    val content: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    val conversation: ConversationEntity,
    val type: Int,
    @OneToMany(
        mappedBy = "chatMessage",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    val messageMedias: MutableList<ChatMessageMediaEntity> = mutableListOf(),
    @Enumerated(EnumType.STRING)
    var reaction: MessageReaction = MessageReaction.NONE,
) : UserAuditedEntity()
