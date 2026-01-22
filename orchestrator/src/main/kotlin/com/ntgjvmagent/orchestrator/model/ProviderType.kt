package com.ntgjvmagent.orchestrator.model

/**
 * Enum to represent different LLM providers supported by the application.
 * Each provider has its own ChatModel implementation and ChatOptions configuration.
 */
enum class ProviderType(
    val displayName: String,
) {
    OPENAI("OpenAI"),
    AZURE_OPENAI("Azure OpenAI"),
    OLLAMA("Ollama"),
    BEDROCK("Amazon Bedrock"),
    ANTHROPIC("Anthropic"),
}
