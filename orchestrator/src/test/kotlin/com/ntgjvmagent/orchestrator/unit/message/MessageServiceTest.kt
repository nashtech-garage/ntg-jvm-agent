package com.ntgjvmagent.orchestrator.unit.message

import com.ntgjvmagent.orchestrator.entity.ChatMessageEntity
import com.ntgjvmagent.orchestrator.entity.ConversationEntity
import com.ntgjvmagent.orchestrator.entity.enums.MessageReaction
import com.ntgjvmagent.orchestrator.exception.BadRequestException
import com.ntgjvmagent.orchestrator.exception.ResourceNotFoundException
import com.ntgjvmagent.orchestrator.repository.ChatMessageRepository
import com.ntgjvmagent.orchestrator.service.MessageService
import com.ntgjvmagent.orchestrator.utils.Constant
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.Optional
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageServiceTest {
    private val messageRepo: ChatMessageRepository = mockk(relaxed = true)
    private val messageService = MessageService(messageRepo)

    private val messageId: UUID = UUID.randomUUID()
    private val userid = UUID.fromString("1ccb35fb-f0ae-4cc4-91fc-8474b3e07475")
    private val reaction = MessageReaction.LIKE

    @BeforeEach
    fun setup() {
        clearMocks(messageRepo)
    }

    private fun createMessage(
        createdBy: UUID?,
        type: Int,
    ): ChatMessageEntity {
        val conversation = mockk<ConversationEntity>(relaxed = true)

        return ChatMessageEntity(
            content = "hello",
            conversation = conversation,
            type = type,
            messageMedias = mutableListOf(),
            reaction = MessageReaction.LIKE,
        ).apply {
            id = messageId
            createdAt = Instant.now()
        }
    }

    @Test
    fun `reactMessage should update reaction when valid`() {
        val message = createMessage(userid, Constant.ANSWER_TYPE)

        every { messageRepo.findById(messageId) } returns Optional.of(message)
        every { messageRepo.save(message) } returns message

        val result = messageService.reactMessage(messageId, reaction, userid)

        assertEquals(messageId, result.id)
        assertEquals("hello", result.content)
        assertEquals(reaction, result.reaction)

        verify(exactly = 1) { messageRepo.findById(messageId) }
        verify(exactly = 1) { messageRepo.save(message) }
    }

    @Test
    fun `reactMessage should throw when message not found`() {
        every { messageRepo.findById(messageId) } returns Optional.empty()

        assertThrows<ResourceNotFoundException> {
            messageService.reactMessage(messageId, reaction, userid)
        }

        verify(exactly = 1) { messageRepo.findById(messageId) }
    }

    @Test
    fun `reactMessage should throw when message type is invalid`() {
        val message = createMessage(userid, Constant.QUESTION_TYPE)

        every { messageRepo.findById(messageId) } returns Optional.of(message)

        assertThrows<BadRequestException> {
            messageService.reactMessage(messageId, reaction, userid)
        }

        verify(exactly = 1) { messageRepo.findById(messageId) }
    }
}
