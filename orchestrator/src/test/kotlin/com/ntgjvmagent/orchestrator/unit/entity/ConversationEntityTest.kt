package com.ntgjvmagent.orchestrator.unit.entity

import com.ntgjvmagent.orchestrator.entity.ChatMessage
import com.ntgjvmagent.orchestrator.entity.Conversation
import com.ntgjvmagent.orchestrator.model.ChatMessageType
import kotlin.test.Test
import kotlin.test.assertEquals

class ConversationEntityTest {
    @Test
    fun `bidirectional messages have finite string representations`() {
        val conversation = Conversation(title = "Sensitive title")
        val message =
            ChatMessage(
                content = "Sensitive content",
                conversation = conversation,
                type = ChatMessageType.QUESTION,
            )
        conversation.messages += message

        assertEquals(
            "Conversation(id=null, sessionId=null, isActive=true)",
            conversation.toString(),
        )
        assertEquals(
            "ChatMessage(id=null, type=QUESTION, reaction=NONE)",
            message.toString(),
        )
    }
}
