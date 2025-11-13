package com.ntgjvmagent.orchestrator.mapper

import com.ntgjvmagent.orchestrator.entity.ChatMessageEntity
import com.ntgjvmagent.orchestrator.viewmodel.ChatMessageMediaVm
import com.ntgjvmagent.orchestrator.viewmodel.ChatMessageResponseVm
import java.util.Base64

object ChatMessageMapper {
    fun toHistoryFormat(entity: ChatMessageEntity): String {
        val role =
            when (entity.type) {
                1 -> "User"
                2 -> "Assistant"
                else -> "Unknown"
            }

        return "$role: ${entity.content}"
    }

    fun toResponse(chatMessage: ChatMessageEntity): ChatMessageResponseVm =
        ChatMessageResponseVm(
            id = chatMessage.id!!,
            content = chatMessage.content,
            createdAt = chatMessage.createdAt,
            type = chatMessage.type,
            medias =
                chatMessage.messageMedias.map { mm ->
                    val base64 = Base64.getEncoder().encodeToString(mm.data)
                    ChatMessageMediaVm(
                        fileName = mm.fileName,
                        contentType = mm.contentType,
                        data = "data:${mm.contentType};base64,$base64",
                    )
                },
        )
}
