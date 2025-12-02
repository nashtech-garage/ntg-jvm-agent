package com.ntgjvmagent.orchestrator.entity

import com.ntgjvmagent.orchestrator.entity.base.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "chat_message")
class ChatMessageEntity(
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
    val messageMedias: MutableSet<ChatMessageMediaEntity> = mutableSetOf(),
    @OneToMany(
        mappedBy = "chatMessage",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    val citations: MutableSet<ChatMessageCitationEntity> = mutableSetOf(),
) : BaseEntity() {
    fun addCitation(
        chunkId: String,
        fileName: String,
        filePath: String,
        charStart: Int,
        charEnd: Int,
    ) {
        val citation =
            ChatMessageCitationEntity(
                chunkId = chunkId,
                fileName = fileName,
                filePath = filePath,
                charStart = charStart,
                charEnd = charEnd,
                chatMessage = this,
            )
        this.citations.add(citation)
    }
}
