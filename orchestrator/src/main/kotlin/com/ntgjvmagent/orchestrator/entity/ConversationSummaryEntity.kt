package com.ntgjvmagent.orchestrator.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Version

@Entity
@Table(name = "conversation_summary")
data class ConversationSummaryEntity(
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // Use conversation.id as this entity's PK
    @JoinColumn(name = "conversation_id")
    val conversation: ConversationEntity,
    @Column(columnDefinition = "TEXT", nullable = false)
    var summaryText: String,
    @Version
    @Column(nullable = false)
    var version: Int = 0,
) : BaseEntity()
