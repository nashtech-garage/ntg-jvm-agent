package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.component.FilteredToolCallbackProvider
import com.ntgjvmagent.orchestrator.component.GlobalToolCallbackProvider
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.repository.AgentToolRepository
import com.ntgjvmagent.orchestrator.utils.Constant
import com.ntgjvmagent.orchestrator.viewmodel.ChatRequestVm
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.vectorstore.filter.Filter
import org.springframework.core.io.InputStreamResource
import org.springframework.stereotype.Service
import org.springframework.util.MimeTypeUtils
import reactor.core.publisher.Flux
import java.util.UUID

@Service
class ChatModelService(
    private val vectorStoreService: VectorStoreService,
    private val agentKnowledgeRepo: AgentKnowledgeRepository,
    private val agentToolRepository: AgentToolRepository,
    private val filteredToolCallbackProvider: FilteredToolCallbackProvider,
    private val globalToolCallbackProvider: GlobalToolCallbackProvider,
    private val dynamicModelService: DynamicModelService,
) {
    private val logger = LoggerFactory.getLogger(ChatModelService::class.java)

    fun call(
        request: ChatRequestVm,
        history: List<String> = emptyList(),
        summary: String = "",
    ): Flux<String> {
        val combinedPrompt =
            buildString {
                appendLine(
                    Constant.SEARCH_TOOL_INSTRUCTION.trimIndent(),
                )
                history.forEach { item ->
                    appendLine(item)
                    if (summary.isNotBlank()) {
                        appendLine("Conversation summary so far:")
                        appendLine(summary)
                        appendLine()
                    }

                    if (history.isNotEmpty()) {
                        appendLine("Chat history:")
                        history.forEach { item ->
                            appendLine(item)
                        }
                        appendLine()
                    }
                }
            }

        val model = dynamicModelService.getChatModel(request.agentId)
        val chatClient = ChatClient.builder(model).build()
        val ragAdvisor = createRagAdvisorForAgent(request.agentId)

        var promptBuilder = chatClient.prompt()
        if (ragAdvisor != null) {
            promptBuilder = promptBuilder.advisors(ragAdvisor)
        }
        return promptBuilder
            .system(Constant.SYSTEM_PROMPT)
            .toolCallbacks(createToolForAgent(request.agentId))
            .user { u ->
                u.text(combinedPrompt)
                request.files
                    ?.filter { !it.isEmpty }
                    ?.forEach { file ->
                        runCatching {
                            val mime =
                                MimeTypeUtils.parseMimeType(
                                    file.contentType ?: Constant.PNG_CONTENT_TYPE,
                                )
                            val resource = InputStreamResource(file.inputStream)
                            u.media(mime, resource)
                        }.onFailure { ex ->
                            logger.warn("Failed to read file ${file.originalFilename}: ${ex.message}")
                        }
                    }
            }.stream()
            .content()
    }

    fun createSummarize(
        agentId: UUID,
        question: String,
    ): String? {
        val prompt =
            Prompt(
                """
                ${Constant.SUMMARY_PROMPT}
                "$question"
                """.trimIndent(),
            )

        val chatModel = dynamicModelService.getChatModel(agentId)
        val response = chatModel.call(prompt)
        return response.result.output.text
    }

    fun createRagAdvisorForAgent(agentId: UUID): RetrievalAugmentationAdvisor? {
        val knowledgeIds: List<String> =
            agentKnowledgeRepo
                .findAllByAgentIdAndActiveTrue(
                    agentId,
                ).map { it.id.toString() }

        if (knowledgeIds.isEmpty()) {
            return null
        }

        val left = Filter.Key("knowledge_id")
        val right = Filter.Value(knowledgeIds)

        val filterExpression =
            Filter.Expression(
                Filter.ExpressionType.IN,
                left,
                right,
            )

        val documentRetriever =
            VectorStoreDocumentRetriever
                .builder()
                .vectorStore(vectorStoreService.getVectorStore(agentId))
                .topK(Constant.TOP_K)
                .filterExpression(filterExpression)
                .build()

        val queryAugmenter: QueryAugmenter =
            ContextualQueryAugmenter
                .builder()
                .allowEmptyContext(true)
                .build()

        return RetrievalAugmentationAdvisor
            .builder()
            .documentRetriever(documentRetriever)
            .queryAugmenter(queryAugmenter)
            .build()
    }

    fun createToolForAgent(agentId: UUID): List<ToolCallback?> {
        val allowedToolNames =
            agentToolRepository.findByAgentId(agentId).map { it.tool.name }

        // Get all globally available tools
        val allCallbacks = globalToolCallbackProvider.getToolCallbacks()

        // Filter them based on agent-config
        return filteredToolCallbackProvider.filterCallbacksByToolNames(
            allCallbacks,
            allowedToolNames,
        )
    }

    fun createDynamicSummary(
        agentId: UUID,
        messagesToSummarize: List<String>,
    ): String {
        if (messagesToSummarize.isEmpty()) return ""

        val joinedMessages = messagesToSummarize.joinToString("\n")

        val promptText =
            Constant.SUMMARY_UPDATE_PROMPT
                .replace("{{latest_message}}", joinedMessages)

        val prompt = Prompt(promptText)
        val chatModel = dynamicModelService.getChatModel(agentId)
        val chatClient = ChatClient.builder(chatModel).build()

        val response =
            chatClient
                .prompt(prompt)
                .call()

        return response.content() ?: ""
    }
}
