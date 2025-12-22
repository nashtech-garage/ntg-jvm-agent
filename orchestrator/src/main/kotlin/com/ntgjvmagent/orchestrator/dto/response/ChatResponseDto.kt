package com.ntgjvmagent.orchestrator.dto.response

import com.ntgjvmagent.orchestrator.viewmodel.ChatMessageResponseVm
import com.ntgjvmagent.orchestrator.viewmodel.ConversationResponseVm

data class ChatResponseDto(
    val conversation: ConversationResponseVm,
    val message: ChatMessageResponseVm?,
)
