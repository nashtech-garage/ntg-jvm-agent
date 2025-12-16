package com.ntgjvmagent.orchestrator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableScheduling
@EnableAsync
@SpringBootApplication(
    exclude = [
        org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration::class,
        org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration::class,
        org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration::class,
        org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration::class,
        org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration::class,
        org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration::class,
        org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration::class,
        org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatAutoConfiguration::class,
        org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration::class,
        org.springframework.ai.model.ollama.autoconfigure.OllamaEmbeddingAutoConfiguration::class,
    ],
)
class OrchestratorApplication

fun main(args: Array<String>) {
    runApplication<OrchestratorApplication>(*args)
}
