package com.ntgjvmagent.orchestrator.viewmodel

data class ChatResponseVm(
    val conversation: ConversationResponseVm,
    val message: ChatMessageResponseVm?,
)
