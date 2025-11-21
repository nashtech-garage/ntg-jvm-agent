package com.ntgjvmagent.orchestrator.entity

import com.ntgjvmagent.orchestrator.entity.base.BaseEntity
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
    var title: String,
    val username: String,
    @Column(name = "is_active")
    var isActive: Boolean = true,
    @OneToMany(
        mappedBy = "conversation",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    val messages: MutableList<ChatMessageEntity> = mutableListOf(),
) : BaseEntity()
