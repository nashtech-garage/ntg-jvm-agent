package com.ntgjvmagent.orchestrator.viewmodel

import java.time.Instant
import java.util.UUID

interface ConversationResponseVm {
    fun getId(): UUID

    fun getTitle(): String

    fun getCreatedAt(): Instant
}

data class ConversationResponseVmImpl(
    private val id: UUID,
    private val title: String,
    private val createdAt: Instant,
) : ConversationResponseVm {
    override fun getId() = id

    override fun getTitle() = title

    override fun getCreatedAt() = createdAt
}
