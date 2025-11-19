package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.component.FilteredToolCallbackProvider
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.repository.AgentToolRepository
import com.ntgjvmagent.orchestrator.utils.Constant
import com.ntgjvmagent.orchestrator.viewmodel.ChatRequestVm
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.core.io.InputStreamResource
import org.springframework.stereotype.Service
import org.springframework.util.MimeTypeUtils
import java.util.UUID

@Service
class ChatModelService(
    private val vectorStoreService: VectorStoreService,
    private val agentKnowledgeRepo: AgentKnowledgeRepository,
    private val agentToolRepo: AgentToolRepository,
    private val filteredToolCallbackProvider: FilteredToolCallbackProvider,
    private val dynamicModelFactory: DynamicModelFactory,
) {
    private val logger = LoggerFactory.getLogger(ChatModelService::class.java)

    fun call(
        request: ChatRequestVm,
        history: List<String> = emptyList(),
    ): String? {
        val combinedPrompt =
            buildString {
                history.forEach { item ->
                    appendLine(item)
                }
                appendLine("User: ${request.question}")
            }

        val model = dynamicModelFactory.getChatModel(request.agentId)
        val chatClient = ChatClient.builder(model).build()
        val qaAdvisor = createQaAdvisorForAgent(request.agentId)

        var promptBuilder = chatClient.prompt()
        if (qaAdvisor != null) {
            promptBuilder = promptBuilder.advisors(qaAdvisor)
        }
        val response =
            promptBuilder
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
                }.call()
                .content()

        return response
    }

    fun createSummarize(question: String): String = question

    fun createQaAdvisorForAgent(agentId: UUID): QuestionAnswerAdvisor? {
        val knowledgeIds: List<String> =
            agentKnowledgeRepo
                .findAllByAgentIdAndActiveTrue(
                    agentId,
                ).map { it.id.toString() }

        if (knowledgeIds.isEmpty()) {
            return null
        }
        val idsArray =
            knowledgeIds.joinToString(
                prefix = "[",
                postfix = "]",
                separator = ",",
            ) { "\"$it\"" }

        val searchRequest =
            SearchRequest
                .builder()
                .topK(Constant.TOP_K)
                .filterExpression("knowledge_id IN $idsArray")
                .build()
        return QuestionAnswerAdvisor
            .builder(vectorStoreService.getVectorStore(agentId))
            .searchRequest(searchRequest)
            .build()
    }

    fun createToolForAgent(agentId: UUID): List<ToolCallback?> {
        val allowedToolNames: List<String> =
            agentToolRepo
                .findByAgentId(agentId)
                .map { it.tool.name }
        return filteredToolCallbackProvider.getCallbacksByToolNames(allowedToolNames)
    }
}
