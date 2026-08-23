package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.component.AgentChatClientFactory
import com.ntgjvmagent.orchestrator.dto.request.ConversationIntentRequestDto
import com.ntgjvmagent.orchestrator.dto.response.ConversationIntentResponseDto
import com.ntgjvmagent.orchestrator.model.TokenOperation
import com.ntgjvmagent.orchestrator.token.accounting.LlmAccountingContext
import com.ntgjvmagent.orchestrator.token.accounting.TokenAccountingFacade
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.ai.converter.StructuredOutputConverter
import org.springframework.stereotype.Service
import tools.jackson.core.JacksonException
import java.util.UUID

@Service
class ConversationIntentService(
    private val chatClientFactory: AgentChatClientFactory,
    private val dynamicChatModelService: DynamicChatModelService,
    private val tokenFacade: TokenAccountingFacade,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val beanOutputConverter = BeanOutputConverter(ConversationIntentResponseDto::class.java)
    private val outputConverter =
        object : StructuredOutputConverter<ConversationIntentResponseDto> {
            override fun getFormat(): String = beanOutputConverter.format

            override fun getJsonSchema(): String = beanOutputConverter.jsonSchema

            override fun convert(source: String): ConversationIntentResponseDto =
                try {
                    beanOutputConverter.convert(source).normalized()
                } catch (exception: JacksonException) {
                    logger.warn("Could not convert conversation intent response", exception)
                    ConversationIntentResponseDto.fallback()
                }
        }

    fun classify(
        userId: UUID,
        request: ConversationIntentRequestDto,
    ): ConversationIntentResponseDto {
        val agentConfig = dynamicChatModelService.getAgentConfig(request.agentId)
        val inputText = "$CLASSIFICATION_PROMPT\n${request.question}\n${outputConverter.format}"
        val estimatedInputTokens = tokenFacade.estimatePromptInput(agentConfig.model, inputText)

        tokenFacade.assertInputBudget(
            userId = userId,
            operation = TokenOperation.CHAT,
            estimatedInputTokens = estimatedInputTokens,
        )

        val responseEntity =
            chatClientFactory
                .create(request.agentId)
                .prompt()
                .system(CLASSIFICATION_PROMPT)
                .user(request.question)
                .call()
                .responseEntity(outputConverter)

        val response = responseEntity.response ?: ChatResponse.builder().build()
        val outputText =
            response
                .result
                ?.output
                ?.text
                .orEmpty()

        tokenFacade.recordWithFallback(
            ctx =
                LlmAccountingContext(
                    userId = userId,
                    agentId = request.agentId,
                    operation = TokenOperation.CHAT,
                    model = agentConfig.model,
                    inputText = inputText,
                    outputText = outputText,
                    estimatedInputTokens = estimatedInputTokens,
                    correlationId = request.correlationId,
                ),
            response = response,
        )

        return responseEntity.entity?.normalized()
            ?: ConversationIntentResponseDto.fallback()
    }

    companion object {
        const val CLASSIFICATION_PROMPT = """
            Classify the user's request for the enterprise assistant.
            category must be GENERAL_CHAT, KNOWLEDGE_LOOKUP, or ACCOUNT_ACTION.
            requiresKnowledge is true only when answering needs attached enterprise knowledge.
            confidence is a number from 0.0 to 1.0.
            rationale is one short sentence explaining the classification.
        """
    }
}
