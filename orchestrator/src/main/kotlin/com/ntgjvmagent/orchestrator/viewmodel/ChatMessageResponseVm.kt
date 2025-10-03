package com.ntgjvmagent.orchestrator.viewmodel

import java.time.ZonedDateTime
import java.util.UUID

interface ChatMessageResponseVm {
    fun getId(): UUID

    fun getContent(): String

    fun getCreatedAt(): ZonedDateTime

    fun getType(): Int
}

data class ChatMessageResponseVmImpl(
    private val id: UUID,
    private val content: String,
    private val createdAt: ZonedDateTime,
    private val type: Int,
) : ChatMessageResponseVm {
    override fun getId() = id

    override fun getContent() = content

    override fun getCreatedAt() = createdAt

    override fun getType() = type
}
