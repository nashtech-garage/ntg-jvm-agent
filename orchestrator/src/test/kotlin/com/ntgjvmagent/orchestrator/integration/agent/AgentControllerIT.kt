package com.ntgjvmagent.orchestrator.integration.agent

import com.fasterxml.jackson.databind.ObjectMapper
import com.ntgjvmagent.orchestrator.dto.request.AgentRequestDto
import com.ntgjvmagent.orchestrator.enum.ProviderType
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@DisplayName("AgentController Integration Tests")
class AgentControllerIT
    @Autowired
    constructor(
        private val mapper: ObjectMapper,
    ) : BaseIntegrationTest() {
        @Test
        @DisplayName("should create and retrieve agent successfully")
        fun shouldCreateAndFetchAgent() {
            val request =
                AgentRequestDto(
                    name = "API Agent",
                    description = "Integration test agent",
                    active = true,
                    provider = ProviderType.OPENAI,
                    baseUrl = "https://models.github.ai/inference",
                    apiKey = "fake-github-token",
                    chatCompletionsPath = "/v1/chat/completions",
                    model = "gpt-4o-mini",
                    embeddingModel = "openai/text-embedding-3-small",
                    dimension = 1536,
                    embeddingsPath = "/embeddings",
                    topP = 1.0,
                    temperature = 0.7,
                    maxTokens = 2048,
                    frequencyPenalty = 0.0,
                    presencePenalty = 0.0,
                )

            val createResult =
                mockMvc
                    .perform(
                        postAuth("/api/agents", request, roles = listOf("SCOPE_chatbot.write")),
                    ).andExpect(status().isCreated)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn()

            val createdJson = createResult.response.contentAsString
            val createdId = mapper.readTree(createdJson).get("id").asText()

            val fetchResult =
                mockMvc
                    .perform(
                        getAuth("/api/agents/$createdId", scopes = listOf("SCOPE_chatbot.read")),
                    ).andExpect(status().isOk)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn()

            val fetchedJson = fetchResult.response.contentAsString
            val fetchedName = mapper.readTree(fetchedJson).get("name").asText()
            assertThat(fetchedName).isEqualTo("API Agent")
        }

        @Test
        @DisplayName("should return 404 for missing agent")
        fun shouldReturn404ForMissingAgent() {
            val randomId = UUID.randomUUID()

            mockMvc
                .perform(
                    getAuth("/api/agents/$randomId", scopes = listOf("SCOPE_chatbot.read")),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error").value("ENTITY_NOT_FOUND"))
        }
    }
