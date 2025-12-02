package com.ntgjvmagent.orchestrator.entity

import com.ntgjvmagent.orchestrator.entity.base.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "chat_message_citation")
data class ChatMessageCitationEntity(
    @Column(nullable = false)
    val chunkId: String,
    @Column(nullable = false)
    val fileName: String,
    @Column(nullable = false)
    val filePath: String,
    @Column(nullable = false)
    val charStart: Int,
    @Column(nullable = false)
    val charEnd: Int,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_message_id", nullable = false)
    val chatMessage: ChatMessageEntity,
) : BaseEntity()
