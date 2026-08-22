package com.ntgjvmagent.orchestrator.unit.chat

import com.ntgjvmagent.orchestrator.chat.handlers.AzureOpenAiChatModelHandler
import com.ntgjvmagent.orchestrator.chat.handlers.OpenAiChatModelHandler
import com.ntgjvmagent.orchestrator.config.LlmProvidersProperties
import com.ntgjvmagent.orchestrator.config.ProviderConfig
import com.ntgjvmagent.orchestrator.embedding.config.EmbeddingModelConfig
import com.ntgjvmagent.orchestrator.embedding.handlers.AzureOpenAiEmbeddingModelHandler
import com.ntgjvmagent.orchestrator.embedding.handlers.OpenAiEmbeddingModelHandler
import com.ntgjvmagent.orchestrator.model.ChatModelConfig
import com.ntgjvmagent.orchestrator.model.ProviderType
import io.micrometer.observation.ObservationRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiEmbeddingModel
import java.math.BigDecimal

class ProviderMigrationTest {
    private val observationRegistry = ObservationRegistry.NOOP

    @Test
    fun `OpenAI compatible chat options preserve endpoint and explicit agent values`() {
        val model =
            OpenAiChatModelHandler(observationRegistry).createChatModel(
                ChatModelConfig(
                    providerType = ProviderType.OPENAI,
                    baseUrl = "https://models.example/inference/",
                    apiKey = "test-key",
                    chatCompletionsPath = "/v1/chat/completions",
                    modelName = "test-chat",
                    temperature = BigDecimal("0.25"),
                    maxTokens = 321,
                ),
            ) as OpenAiChatModel

        with(model.options) {
            assertEquals("https://models.example/inference/v1", baseUrl)
            assertEquals("test-key", apiKey)
            assertEquals("test-chat", this.model)
            assertEquals(0.25, temperature)
            assertEquals(321, maxTokens)
            assertEquals(0, maxRetries)
            assertFalse(isMicrosoftFoundry)
        }
    }

    @Test
    fun `OpenAI compatible chat options retain provider defaults when agent values are absent`() {
        val model =
            OpenAiChatModelHandler(observationRegistry).createChatModel(
                ChatModelConfig(
                    providerType = ProviderType.OPENAI,
                    baseUrl = "https://api.example/v1",
                    apiKey = "test-key",
                    chatCompletionsPath = "/chat/completions",
                    modelName = "test-chat",
                ),
            ) as OpenAiChatModel

        assertNull(model.options.temperature)
        assertNull(model.options.maxTokens)
    }

    @Test
    fun `Azure chat uses unified OpenAI Azure options and deployment`() {
        val model =
            AzureOpenAiChatModelHandler(observationRegistry).createChatModel(
                ChatModelConfig(
                    providerType = ProviderType.AZURE_OPENAI,
                    baseUrl = "https://resource.openai.azure.com",
                    apiKey = "azure-key",
                    chatCompletionsPath = "/ignored",
                    modelName = "chat-deployment",
                ),
            ) as OpenAiChatModel

        with(model.options) {
            assertEquals("https://resource.openai.azure.com", baseUrl)
            assertEquals("chat-deployment", deploymentName)
            assertEquals("chat-deployment", this.model)
            assertTrue(isMicrosoftFoundry)
            assertEquals(0, maxRetries)
        }
    }

    @Test
    fun `embedding handlers use unified OpenAI options for compatible and Azure endpoints`() {
        val properties =
            LlmProvidersProperties(
                providers =
                    mapOf(
                        ProviderType.OPENAI to
                            ProviderConfig(
                                baseUrl = "https://models.example/inference",
                                apiKey = "openai-key",
                                embeddingsPath = "/v1/embeddings",
                            ),
                        ProviderType.AZURE_OPENAI to
                            ProviderConfig(
                                baseUrl = "https://resource.openai.azure.com",
                                apiKey = "azure-key",
                            ),
                    ),
            )

        val openAi =
            OpenAiEmbeddingModelHandler(properties, observationRegistry)
                .createEmbeddingModel(EmbeddingModelConfig(ProviderType.OPENAI, "text-embedding")) as
                OpenAiEmbeddingModel
        val azure =
            AzureOpenAiEmbeddingModelHandler(properties, observationRegistry)
                .createEmbeddingModel(EmbeddingModelConfig(ProviderType.AZURE_OPENAI, "embedding-deployment")) as
                OpenAiEmbeddingModel

        with(openAi.options) {
            assertEquals("https://models.example/inference/v1", baseUrl)
            assertEquals("text-embedding", model)
            assertFalse(isMicrosoftFoundry)
            assertEquals(0, maxRetries)
        }
        with(azure.options) {
            assertEquals("https://resource.openai.azure.com", baseUrl)
            assertEquals("embedding-deployment", deploymentName)
            assertTrue(isMicrosoftFoundry)
            assertEquals(0, maxRetries)
        }
    }
}
