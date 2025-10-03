package com.ntgjvmagent.orchestrator.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "conversation")
data class ConversationEntity(
    @Column(columnDefinition = "TEXT")
    val title: String,
    val username: String,
    @Column(name = "is_active")
    val isActive: Boolean = true,
    @OneToMany(
        mappedBy = "conversation",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    val messages: MutableList<ChatMessageEntity> = mutableListOf(),
) : BaseEntity()
