package com.ntgjvmagent.orchestrator

import org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatAutoConfiguration
import org.springframework.ai.model.bedrock.converse.autoconfigure.BedrockConverseProxyChatAutoConfiguration
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration
import org.springframework.ai.model.ollama.autoconfigure.OllamaEmbeddingAutoConfiguration
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration
import org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration
import org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableScheduling
@EnableAsync
@SpringBootApplication(
    exclude = [
        OpenAiAudioSpeechAutoConfiguration::class,
        OpenAiEmbeddingAutoConfiguration::class,
        OpenAiAudioTranscriptionAutoConfiguration::class,
        OpenAiChatAutoConfiguration::class,
        OpenAiImageAutoConfiguration::class,
        OpenAiModerationAutoConfiguration::class,
        PgVectorStoreAutoConfiguration::class,
        AnthropicChatAutoConfiguration::class,
        OllamaChatAutoConfiguration::class,
        OllamaEmbeddingAutoConfiguration::class,
        BedrockConverseProxyChatAutoConfiguration::class,
    ],
)
@ConfigurationPropertiesScan(basePackages = ["com.ntgjvmagent.orchestrator.config"])
class OrchestratorApplication

fun main(args: Array<String>) {
    runApplication<OrchestratorApplication>(*args)
}
