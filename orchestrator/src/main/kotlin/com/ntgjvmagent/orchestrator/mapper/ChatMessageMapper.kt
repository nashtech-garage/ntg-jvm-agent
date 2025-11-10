package com.ntgjvmagent.orchestrator.mapper

import com.ntgjvmagent.orchestrator.viewmodel.ChatMessageResponseVm

object ChatMessageMapper {
    fun toHistoryFormat(entity: ChatMessageResponseVm): String {
        val role =
            when (entity.getType()) {
                1 -> "User"
                2 -> "Assistant"
                else -> "Unknown"
            }

        return "$role: ${entity.getContent()}"
    }
}
