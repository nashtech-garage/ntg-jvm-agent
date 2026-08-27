package com.ntgjvmagent.orchestrator.unit.config

import com.ntgjvmagent.orchestrator.config.EmbeddingProperties
import com.ntgjvmagent.orchestrator.config.LlmConfigStartupValidator
import com.ntgjvmagent.orchestrator.config.LlmProvidersProperties
import com.ntgjvmagent.orchestrator.config.ProviderConfig
import com.ntgjvmagent.orchestrator.model.ProviderType
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class LlmConfigStartupValidatorTest {
    @Test
    fun `empty API key permits structural local deployment`() {
        validator(apiKey = "").afterSingletonsInstantiated()
    }

    @Test
    fun `invalid embedding shape still fails startup`() {
        assertFailsWith<IllegalArgumentException> {
            validator(model = "").afterSingletonsInstantiated()
        }
        assertFailsWith<IllegalArgumentException> {
            validator(dimension = 0).afterSingletonsInstantiated()
        }
    }

    @Test
    fun `missing embedding provider still fails startup`() {
        val validator =
            LlmConfigStartupValidator(
                embeddingProps =
                    EmbeddingProperties(
                        provider = ProviderType.OPENAI,
                        model = "text-embedding-3-small",
                        dimension = 1536,
                    ),
                llmProviders = LlmProvidersProperties(emptyMap()),
            )

        assertFailsWith<IllegalStateException> {
            validator.afterSingletonsInstantiated()
        }
    }

    private fun validator(
        apiKey: String = "test-key",
        model: String = "text-embedding-3-small",
        dimension: Int = 1536,
    ): LlmConfigStartupValidator =
        LlmConfigStartupValidator(
            embeddingProps =
                EmbeddingProperties(
                    provider = ProviderType.OPENAI,
                    model = model,
                    dimension = dimension,
                ),
            llmProviders =
                LlmProvidersProperties(
                    mapOf(
                        ProviderType.OPENAI to
                            ProviderConfig(
                                baseUrl = "https://api.openai.com/v1",
                                apiKey = apiKey,
                            ),
                    ),
                ),
        )
}
