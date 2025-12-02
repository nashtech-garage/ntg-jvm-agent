package com.ntgjvmagent.orchestrator.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.ntgjvmagent.orchestrator.entity.ChatMessageEntity
import com.ntgjvmagent.orchestrator.entity.ConversationEntity
import com.ntgjvmagent.orchestrator.entity.ConversationShareEntity
import com.ntgjvmagent.orchestrator.repository.ChatMessageRepository
import com.ntgjvmagent.orchestrator.repository.ConversationRepository
import com.ntgjvmagent.orchestrator.repository.ConversationShareRepository
import com.ntgjvmagent.orchestrator.viewmodel.ShareConversationRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.OffsetDateTime
import java.util.UUID

@DisplayName("SharedConversationController Integration Tests")
class SharedConversationControllerIT @Autowired constructor(
    private val mapper: ObjectMapper,
    private val conversationRepository: ConversationRepository,
    private val conversationShareRepository: ConversationShareRepository,
    private val chatMessageRepository: ChatMessageRepository,
) : BaseIntegrationTest() {
    private val testUsername = "testuser@example.com"
    private val otherUsername = "otheruser@example.com"
    private var testConversationId: UUID? = null
    private var testShareToken: String? = null

    @BeforeEach
    fun setup() {
        conversationShareRepository.deleteAll()
        chatMessageRepository.deleteAll()
        conversationRepository.deleteAll()
        testConversationId = null
        testShareToken = null
    }

    @Test
    @DisplayName("should share a conversation with valid request")
    fun shouldShareConversation() {
        // Setup: Create a conversation
        val conversation = ConversationEntity(
            title = "Shared Conversation",
            username = testUsername,
        )
        val savedConversation = conversationRepository.save(conversation)
        testConversationId = savedConversation.id

        // Create some messages
        val message1 = ChatMessageEntity(
            conversation = savedConversation,
            content = "Test message 1",
            type = 1, // USER
        )
        val message2 = ChatMessageEntity(
            conversation = savedConversation,
            content = "Test response 1",
            type = 2, // ASSISTANT
        )
        chatMessageRepository.save(message1)
        chatMessageRepository.save(message2)

        val shareRequest = ShareConversationRequest(expiryDays = 7)

        // Execute: Share the conversation
        val result = mockMvc
            .perform(
                postAuth(
                    "/api/share/shared-conversations/${savedConversation.id}/share",
                    shareRequest,
                )
            )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.conversationId").value(savedConversation.id.toString()))
            .andExpect(jsonPath("$.conversationTitle").value("Shared Conversation"))
            .andExpect(jsonPath("$.sharedByUsername").value(testUsername))
            .andExpect(jsonPath("$.shareToken").isNotEmpty)
            .andExpect(jsonPath("$.isExpired").value(false as Any))
            .andExpect(jsonPath("$.expiresAt").isNotEmpty)
            .andReturn()

        val responseJson = result.response.contentAsString
        testShareToken = mapper.readTree(responseJson).get("shareToken").asText()
        assertThat(testShareToken).isNotNull
    }

    @Test
    @DisplayName("should not allow sharing conversation owned by other user")
    fun shouldNotShareConversationOwnedByOther() {
        // Setup: Create a conversation owned by other user
        val conversation = ConversationEntity(
            title = "Other User's Conversation",
            username = otherUsername,
        )
        val savedConversation = conversationRepository.save(conversation)

        val shareRequest = ShareConversationRequest(expiryDays = 7)

        // Execute: Try to share the conversation with different user
        mockMvc
            .perform(
                postAuth(
                    "/api/share/shared-conversations/${savedConversation.id}/share",
                    shareRequest,
                )
            )
            .andExpect(status().isBadRequest)
            .andReturn()
    }

    @Test
    @DisplayName("should fail to share non-existent conversation")
    fun shouldFailToShareNonExistentConversation() {
        val nonExistentId = UUID.randomUUID()
        val shareRequest = ShareConversationRequest(expiryDays = 7)

        mockMvc
            .perform(
                postAuth(
                    "/api/share/shared-conversations/$nonExistentId/share",
                    shareRequest,
                )
            )
            .andExpect(status().isNotFound)
            .andReturn()
    }

    @Test
    @DisplayName("should validate share request expiry days minimum")
    fun shouldValidateMinimumExpiryDays() {
        val conversation = ConversationEntity(
            title = "Test Conversation",
            username = testUsername,
        )
        val savedConversation = conversationRepository.save(conversation)

        val invalidShareRequest = ShareConversationRequest(expiryDays = 0)

        mockMvc
            .perform(
                postAuth(
                    "/api/share/shared-conversations/${savedConversation.id}/share",
                    invalidShareRequest,
                )
            )
            .andExpect(status().isBadRequest)
            .andReturn()
    }

    @Test
    @DisplayName("should validate share request expiry days maximum")
    fun shouldValidateMaximumExpiryDays() {
        val conversation = ConversationEntity(
            title = "Test Conversation",
            username = testUsername,
        )
        val savedConversation = conversationRepository.save(conversation)

        val invalidShareRequest = ShareConversationRequest(expiryDays = 91)

        mockMvc
            .perform(
                postAuth(
                    "/api/share/shared-conversations/${savedConversation.id}/share",
                    invalidShareRequest,
                )
            )
            .andExpect(status().isBadRequest)
            .andReturn()
    }

    @Test
    @DisplayName("should get shared conversation by token (no auth required)")
    fun shouldGetSharedConversationByToken() {
        // Setup: Create conversation with share
        val conversation = ConversationEntity(
            title = "Public Shared Conversation",
            username = testUsername,
        )
        val savedConversation = conversationRepository.save(conversation)

        val message1 = ChatMessageEntity(
            conversation = savedConversation,
            content = "Test message",
            type = 1, // USER
        )
        val message2 = ChatMessageEntity(
            conversation = savedConversation,
            content = "Test response",
            type = 2, // ASSISTANT
        )
        val msg1 = chatMessageRepository.save(message1)
        val msg2 = chatMessageRepository.save(message2)

        val shareEntity = ConversationShareEntity(
            conversation = savedConversation,
            sharedByUsername = testUsername,
            shareToken = "test-share-token-${UUID.randomUUID()}",
            isExpired = false,
            expiresAt = OffsetDateTime.now().plusDays(7),
            sharedMessageIds = listOf(msg1.id.toString(), msg2.id.toString()),
        )
        val savedShare = conversationShareRepository.save(shareEntity)

        // Execute: Get shared conversation with token (no auth needed)
        mockMvc
            .perform(
                get("/api/share/shared-conversations/${savedShare.shareToken}")
            )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(savedConversation.id.toString()))
            .andExpect(jsonPath("$.title").value("Public Shared Conversation"))
            .andExpect(jsonPath("$.sharedByUsername").value(testUsername))
            .andExpect(jsonPath("$.messages").isArray)
            .andExpect(jsonPath("$.messages.length()").value(2))
            .andReturn()
    }

    @Test
    @DisplayName("should return 404 for invalid share token")
    fun shouldReturn404ForInvalidShareToken() {
        mockMvc
            .perform(
                get("/api/share/shared-conversations/invalid-token-${UUID.randomUUID()}")
            )
            .andExpect(status().isNotFound)
            .andReturn()
    }

    @Test
    @DisplayName("should return 400 for revoked share token")
    fun shouldReturn400ForRevokedShareToken() {
        // Setup: Create and revoke a share
        val conversation = ConversationEntity(
            title = "Test Conversation",
            username = testUsername,
        )
        val savedConversation = conversationRepository.save(conversation)

        val revokedShare = ConversationShareEntity(
            conversation = savedConversation,
            sharedByUsername = testUsername,
            shareToken = "revoked-token-${UUID.randomUUID()}",
            isExpired = true,
            expiresAt = OffsetDateTime.now().plusDays(7),
            sharedMessageIds = emptyList(),
        )
        val savedShare = conversationShareRepository.save(revokedShare)

        // Execute: Try to access revoked share
        mockMvc
            .perform(
                get("/api/share/shared-conversations/${savedShare.shareToken}")
            )
            .andExpect(status().isBadRequest)
            .andReturn()
    }

    @Test
    @DisplayName("should return 400 for expired share token")
    fun shouldReturn400ForExpiredShareToken() {
        // Setup: Create an expired share
        val conversation = ConversationEntity(
            title = "Test Conversation",
            username = testUsername,
        )
        val savedConversation = conversationRepository.save(conversation)

        val expiredShare = ConversationShareEntity(
            conversation = savedConversation,
            sharedByUsername = testUsername,
            shareToken = "expired-token-${UUID.randomUUID()}",
            isExpired = false,
            expiresAt = OffsetDateTime.now().minusDays(1), // Expired
            sharedMessageIds = emptyList(),
        )
        val savedShare = conversationShareRepository.save(expiredShare)

        // Execute: Try to access expired share
        mockMvc
            .perform(
                get("/api/share/shared-conversations/${savedShare.shareToken}")
            )
            .andExpect(status().isBadRequest)
            .andReturn()
    }

    @Test
    @DisplayName("should list all shares for a conversation")
    fun shouldListConversationShares() {
        // Setup: Create conversation and multiple shares
        val conversation = ConversationEntity(
            title = "Conversation with Multiple Shares",
            username = testUsername,
        )
        val savedConversation = conversationRepository.save(conversation)

        val share1 = ConversationShareEntity(
            conversation = savedConversation,
            sharedByUsername = testUsername,
            shareToken = "token1-${UUID.randomUUID()}",
            isExpired = false,
            expiresAt = OffsetDateTime.now().plusDays(7),
            sharedMessageIds = emptyList(),
        )
        val share2 = ConversationShareEntity(
            conversation = savedConversation,
            sharedByUsername = testUsername,
            shareToken = "token2-${UUID.randomUUID()}",
            isExpired = false,
            expiresAt = OffsetDateTime.now().plusDays(14),
            sharedMessageIds = emptyList(),
        )
        conversationShareRepository.save(share1)
        conversationShareRepository.save(share2)

        // Execute: List all shares
        mockMvc
            .perform(
                getAuth("/api/share/shared-conversations/conversation/${savedConversation.id}/shares")
            )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(2 as Any))
            .andExpect(jsonPath("$[0].conversationId").value(savedConversation.id.toString()))
            .andExpect(jsonPath("$[1].conversationId").value(savedConversation.id.toString()))
            .andReturn()
    }

    @Test
    @DisplayName("should not allow listing shares for conversation owned by other user")
    fun shouldNotListSharesForOtherUsersConversation() {
        // Setup: Create conversation owned by other user
        val conversation = ConversationEntity(
            title = "Other User's Conversation",
            username = otherUsername,
        )
        val savedConversation = conversationRepository.save(conversation)

        // Execute: Try to list shares with different user
        mockMvc
            .perform(
                getAuth("/api/share/shared-conversations/conversation/${savedConversation.id}/shares")
            )
            .andExpect(status().isBadRequest)
            .andReturn()
    }

    @Test
    @DisplayName("should return empty list when conversation has no shares")
    fun shouldReturnEmptyListForNoShares() {
        // Setup: Create conversation without shares
        val conversation = ConversationEntity(
            title = "Conversation without Shares",
            username = testUsername,
        )
        val savedConversation = conversationRepository.save(conversation)

        // Execute: List shares
        mockMvc
            .perform(
                getAuth("/api/share/shared-conversations/conversation/${savedConversation.id}/shares")
            )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(0 as Any))
            .andReturn()
    }

    @Test
    @DisplayName("should revoke a share")
    fun shouldRevokeShare() {
        // Setup: Create conversation and share
        val conversation = ConversationEntity(
            title = "Conversation to Revoke Share",
            username = testUsername,
        )
        val savedConversation = conversationRepository.save(conversation)

        val share = ConversationShareEntity(
            conversation = savedConversation,
            sharedByUsername = testUsername,
            shareToken = "revoke-me-${UUID.randomUUID()}",
            isExpired = false,
            expiresAt = OffsetDateTime.now().plusDays(7),
            sharedMessageIds = emptyList(),
        )
        val savedShare = conversationShareRepository.save(share)

        // Execute: Revoke the share
        mockMvc
            .perform(
                deleteAuth("/api/share/shared-conversations/${savedShare.shareToken}")
            )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.shareToken").value(savedShare.shareToken))
            .andExpect(jsonPath("$.isExpired").value(true as Any))
            .andReturn()

        // Verify: Share is marked as expired
        val revokedShare = conversationShareRepository.findByShareToken(savedShare.shareToken)
        assertThat(revokedShare.isPresent).isTrue
        assertThat(revokedShare.get().isExpired).isTrue
    }

    @Test
    @DisplayName("should not allow revoking share owned by other user")
    fun shouldNotRevokeShareOwnedByOther() {
        // Setup: Create share owned by other user
        val conversation = ConversationEntity(
            title = "Test Conversation",
            username = otherUsername,
        )
        val savedConversation = conversationRepository.save(conversation)

        val share = ConversationShareEntity(
            conversation = savedConversation,
            sharedByUsername = otherUsername,
            shareToken = "other-user-token-${UUID.randomUUID()}",
            isExpired = false,
            expiresAt = OffsetDateTime.now().plusDays(7),
            sharedMessageIds = emptyList(),
        )
        val savedShare = conversationShareRepository.save(share)

        // Execute: Try to revoke with different user
        mockMvc
            .perform(
                deleteAuth("/api/share/shared-conversations/${savedShare.shareToken}")
            )
            .andExpect(status().isBadRequest)
            .andReturn()
    }

    @Test
    @DisplayName("should return 404 when revoking non-existent share")
    fun shouldReturn404WhenRevokingNonExistentShare() {
        mockMvc
            .perform(
                deleteAuth("/api/share/shared-conversations/non-existent-token")
            )
            .andExpect(status().isNotFound)
            .andReturn()
    }

    @Test
    @DisplayName("should share conversation with default expiry days")
    fun shouldShareConversationWithDefaultExpiryDays() {
        // Setup: Create a conversation
        val conversation = ConversationEntity(
            title = "Conversation with Default Expiry",
            username = testUsername,
        )
        val savedConversation = conversationRepository.save(conversation)

        // Use default expiry days (7 days)
        val shareRequest = ShareConversationRequest()

        // Execute: Share the conversation
        mockMvc
            .perform(
                postAuth(
                    "/api/share/shared-conversations/${savedConversation.id}/share",
                    shareRequest,
                )
            )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.expiresAt").isNotEmpty)
            .andReturn()
    }

    @Test
    @DisplayName("should get shared conversation with correct message filtering")
    fun shouldGetSharedConversationWithMessageFiltering() {
        // Setup: Create conversation with multiple messages
        val conversation = ConversationEntity(
            title = "Filtered Messages Conversation",
            username = testUsername,
        )
        val savedConversation = conversationRepository.save(conversation)

        val message1 = ChatMessageEntity(
            conversation = savedConversation,
            content = "Message 1",
            type = 1, // USER
        )
        val message2 = ChatMessageEntity(
            conversation = savedConversation,
            content = "Response 1",
            type = 2, // ASSISTANT
        )
        val message3 = ChatMessageEntity(
            conversation = savedConversation,
            content = "Message 2",
            type = 1, // USER
        )
        val msg1 = chatMessageRepository.save(message1)
        val msg2 = chatMessageRepository.save(message2)
        chatMessageRepository.save(message3)

        // Share only first two messages
        val shareEntity = ConversationShareEntity(
            conversation = savedConversation,
            sharedByUsername = testUsername,
            shareToken = "filtered-${UUID.randomUUID()}",
            isExpired = false,
            expiresAt = OffsetDateTime.now().plusDays(7),
            sharedMessageIds = listOf(msg1.id.toString(), msg2.id.toString()),
        )
        val savedShare = conversationShareRepository.save(shareEntity)

        // Execute: Get shared conversation
        mockMvc
            .perform(
                get("/api/share/shared-conversations/${savedShare.shareToken}")
            )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.messages.length()").value(2 as Any))
            .andReturn()
    }
}

