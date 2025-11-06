package com.ntgjvmagent.orchestrator.integration.agent

import com.fasterxml.jackson.databind.ObjectMapper
import com.ntgjvmagent.orchestrator.dto.AgentRequestDto
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@AutoConfigureMockMvc
@DisplayName("AgentController Integration Tests")
class AgentControllerIT
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val mapper: ObjectMapper,
    ) : BaseIntegrationTest() {
        @Test
        @DisplayName("should create and retrieve agent successfully")
        fun shouldCreateAndFetchAgent() {
            val request =
                AgentRequestDto(
                    name = "API Agent",
                    model = "gpt-4o-mini",
                    description = "Integration test agent",
                    temperature = 0.7,
                    maxTokens = 2048,
                    topP = 1.0,
                    frequencyPenalty = 0.0,
                    presencePenalty = 0.0,
                    active = true,
                )

            val createResult =
                mockMvc
                    .perform(
                        post("/api/agents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(request))
                            .with(jwt().authorities(SimpleGrantedAuthority("SCOPE_chatbot.write"))),
                    ).andExpect(status().isCreated)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn()

            val createdJson = createResult.response.contentAsString
            val createdId = mapper.readTree(createdJson).get("id").asText()

            val fetchResult =
                mockMvc
                    .perform(
                        get(
                            "/api/agents/$createdId",
                        ).with(jwt().authorities(SimpleGrantedAuthority("SCOPE_chatbot.read"))),
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
                    get("/api/agents/$randomId").with(jwt().authorities(SimpleGrantedAuthority("SCOPE_chatbot.read"))),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error").value("Not Found"))
        }
    }
