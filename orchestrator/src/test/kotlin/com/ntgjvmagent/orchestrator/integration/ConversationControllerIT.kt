package com.ntgjvmagent.orchestrator.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.ntgjvmagent.orchestrator.entity.ConversationEntity
import com.ntgjvmagent.orchestrator.repository.ConversationRepository
import com.ntgjvmagent.orchestrator.viewmodel.ConversationUpdateRequestVm
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@DisplayName("ConversationController Integration Tests")
class ConversationControllerIT @Autowired constructor(
    private val mapper: ObjectMapper,
    private val conversationRepository: ConversationRepository,
) : BaseIntegrationTest() {
    private val testUsername = "testuser@example.com"
    private var testConversationId: UUID? = null

    @BeforeEach
    fun setup() {
        conversationRepository.deleteAll()
        testConversationId = null
    }

    @Test
    @DisplayName("should create a new conversation with chat request")
    fun shouldCreateNewConversation() {
        val agentId = UUID.randomUUID()
        val question = "What is Kotlin?"

        val file =
            MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "test content".toByteArray(),
            )

        val requestBuilder =
            multipartAuth("/api/conversations", file)
                .param("agentId", agentId.toString())
                .param("question", question)

        val result =
            mockMvc
                .perform(requestBuilder)
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()

        val responseJson = result.response.contentAsString
        val responseTree = mapper.readTree(responseJson)

        assertThat(responseTree.get("conversationId")).isNotNull
        assertThat(responseTree.get("title")).isNotNull
        assertThat(responseTree.get("answer")).isNotNull

        testConversationId = UUID.fromString(responseTree.get("conversationId").asText())
    }

    @Test
    @DisplayName("should create conversation without file")
    fun shouldCreateConversationWithoutFile() {
        val agentId = UUID.randomUUID()
        val question = "Explain Spring Boot"

        val file = MockMultipartFile("file", "".toByteArray())

        val requestBuilder =
            multipartAuth("/api/conversations", file)
                .param("agentId", agentId.toString())
                .param("question", question)

        val result =
            mockMvc
                .perform(requestBuilder)
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()

        val responseJson = result.response.contentAsString
        val conversationId = mapper.readTree(responseJson).get("conversationId").asText()
        testConversationId = UUID.fromString(conversationId)

        assertThat(testConversationId).isNotNull
    }

    @Test
    @DisplayName("should list conversations for authenticated user")
    fun shouldListUserConversations() {
        val conversation1 =
            ConversationEntity(
                title = "Test Conversation 1",
                username = testUsername,
            )
        val conversation2 =
            ConversationEntity(
                title = "Test Conversation 2",
                username = testUsername,
            )
        val conversation3 =
            ConversationEntity(
                title = "Other User Conversation",
                username = "other@example.com",
            )

        conversationRepository.save(conversation1)
        conversationRepository.save(conversation2)
        conversationRepository.save(conversation3)

        val result =
            mockMvc
                .perform(getAuth("/api/conversations/user"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()

        val responseJson = result.response.contentAsString
        val conversations = mapper.readTree(responseJson)

        assertThat(conversations.size()).isGreaterThanOrEqualTo(2)
    }

    @Test
    @DisplayName("should get messages from a conversation")
    fun shouldGetConversationMessages() {
        val conversation =
            ConversationEntity(
                title = "Test Conversation with Messages",
                username = testUsername,
            )
        val savedConversation = conversationRepository.save(conversation)

        val result =
            mockMvc
                .perform(getAuth("/api/conversations/${savedConversation.id}/messages"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()

        val responseJson = result.response.contentAsString
        val messages = mapper.readTree(responseJson)

        assertThat(messages.size()).isGreaterThanOrEqualTo(0)
    }

    @Test
    @DisplayName("should return 404 when getting messages from non-existent conversation")
    fun shouldReturn404ForNonExistentConversation() {
        val nonExistentId = UUID.randomUUID()

        mockMvc
            .perform(getAuth("/api/conversations/$nonExistentId/messages"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Not Found"))
    }

    @Test
    @DisplayName("should delete a conversation")
    fun shouldDeleteConversation() {
        val conversation =
            ConversationEntity(
                title = "Conversation to Delete",
                username = testUsername,
            )
        val savedConversation = conversationRepository.save(conversation)

        mockMvc
            .perform(deleteAuth("/api/conversations/${savedConversation.id}"))
            .andExpect(status().isNoContent)
            .andReturn()

        val deletedConversation = conversationRepository.findById(savedConversation.id!!)
        assertThat(deletedConversation.isPresent).isTrue
        assertThat(deletedConversation.get().isActive).isFalse
    }

    @Test
    @DisplayName("should return 404 when deleting non-existent conversation")
    fun shouldReturn404WhenDeletingNonExistentConversation() {
        val nonExistentId = UUID.randomUUID()

        mockMvc
            .perform(deleteAuth("/api/conversations/$nonExistentId"))
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("should update conversation title")
    fun shouldUpdateConversationTitle() {
        val conversation =
            ConversationEntity(
                title = "Old Title",
                username = testUsername,
            )
        val savedConversation = conversationRepository.save(conversation)

        val updateRequest = ConversationUpdateRequestVm(title = "New Title")

        val result =
            mockMvc
                .perform(
                    putAuth("/api/conversations/${savedConversation.id}", updateRequest),
                )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()

        val responseJson = result.response.contentAsString
        val updatedTitle = mapper.readTree(responseJson).get("title").asText()

        assertThat(updatedTitle).isEqualTo("New Title")

        val updatedConversation = conversationRepository.findById(savedConversation.id!!)
        assertThat(updatedConversation.get().title).isEqualTo("New Title")
    }

    @Test
    @DisplayName("should trim whitespace from updated title")
    fun shouldTrimWhitespaceFromTitle() {
        val conversation =
            ConversationEntity(
                title = "Old Title",
                username = testUsername,
            )
        val savedConversation = conversationRepository.save(conversation)

        val updateRequest =
            ConversationUpdateRequestVm(
                title = "   New Title with Spaces   ",
            )

        val result =
            mockMvc
                .perform(
                    putAuth("/api/conversations/${savedConversation.id}", updateRequest),
                )
                .andExpect(status().isOk)
                .andReturn()

        val responseJson = result.response.contentAsString
        val updatedTitle = mapper.readTree(responseJson).get("title").asText()

        assertThat(updatedTitle).isEqualTo("New Title with Spaces")
    }

    @Test
    @DisplayName("should prevent unauthorized user from updating conversation")
    fun shouldPreventUnauthorizedUpdate() {
        val otherUsername = "otheruser@example.com"
        val conversation =
            ConversationEntity(
                title = "Protected Conversation",
                username = otherUsername,
            )
        val savedConversation = conversationRepository.save(conversation)

        val updateRequest = ConversationUpdateRequestVm(title = "Hacked Title")

        mockMvc
            .perform(
                putAuth("/api/conversations/${savedConversation.id}", updateRequest),
            )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Not Found"))
    }

    @Test
    @DisplayName("should return 404 when updating non-existent conversation")
    fun shouldReturn404WhenUpdatingNonExistentConversation() {
        val nonExistentId = UUID.randomUUID()
        val updateRequest = ConversationUpdateRequestVm(title = "New Title")

        mockMvc
            .perform(
                putAuth("/api/conversations/$nonExistentId", updateRequest),
            )
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("should list only active conversations for user")
    fun shouldListOnlyActiveConversations() {
        val activeConversation =
            ConversationEntity(
                title = "Active Conversation",
                username = testUsername,
                isActive = true,
            )
        conversationRepository.save(activeConversation)

        val inactiveConversation =
            ConversationEntity(
                title = "Inactive Conversation",
                username = testUsername,
                isActive = false,
            )
        conversationRepository.save(inactiveConversation)

        val result =
            mockMvc
                .perform(getAuth("/api/conversations/user"))
                .andExpect(status().isOk)
                .andReturn()

        val responseJson = result.response.contentAsString
        val conversations = mapper.readTree(responseJson)

        val titles = conversations.map { it.get("title").asText() }
        assertThat(titles).contains("Active Conversation")
        assertThat(titles).doesNotContain("Inactive Conversation")
    }

    @Test
    @DisplayName("should handle empty conversation list")
    fun shouldHandleEmptyConversationList() {
        mockMvc
            .perform(getAuth("/api/conversations/user"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn()
    }
}
