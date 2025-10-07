package com.ntgjvmagent.orchestrator.viewmodel

import java.time.ZonedDateTime
import java.util.UUID

interface ConversationResponseVm {
    fun getId(): UUID

    fun getTitle(): String

    fun getCreatedAt(): ZonedDateTime
}

data class ConversationResponseVmImpl(
    private val id: UUID,
    private val title: String,
    private val createdAt: ZonedDateTime,
) : ConversationResponseVm {
    override fun getId() = id

    override fun getTitle() = title

    override fun getCreatedAt() = createdAt
}
