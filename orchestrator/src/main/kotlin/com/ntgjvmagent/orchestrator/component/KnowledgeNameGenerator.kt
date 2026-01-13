package com.ntgjvmagent.orchestrator.component

import com.ntgjvmagent.orchestrator.dto.request.AgentKnowledgeRequestDto
import com.ntgjvmagent.orchestrator.dto.request.ApiKnowledgeRequestDto
import com.ntgjvmagent.orchestrator.dto.request.DatabaseKnowledgeRequestDto
import com.ntgjvmagent.orchestrator.dto.request.InlineKnowledgeRequestDto
import com.ntgjvmagent.orchestrator.dto.request.SitemapKnowledgeRequestDto
import com.ntgjvmagent.orchestrator.dto.request.UrlKnowledgeRequestDto
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import org.springframework.stereotype.Component
import java.net.URI
import java.util.UUID

@Component
class KnowledgeNameGenerator(
    private val repo: AgentKnowledgeRepository,
) {
    fun generate(
        agentId: UUID,
        request: AgentKnowledgeRequestDto,
    ): String {
        val base =
            when (request) {
                is DatabaseKnowledgeRequestDto -> {
                    "DB: ${request.dbHost}:${request.dbPort}"
                }

                is ApiKnowledgeRequestDto -> {
                    buildApiName(request)
                }

                is UrlKnowledgeRequestDto -> {
                    normalizeUrl(request.sourceUri)
                }

                is SitemapKnowledgeRequestDto -> {
                    "Sitemap: ${extractHost(request.sourceUri)}"
                }

                is InlineKnowledgeRequestDto -> {
                    "Inline Content"
                }
            }

        return uniquify(agentId, base)
    }

    fun generateForFile(
        agentId: UUID,
        originalFileName: String,
    ): String = uniquify(agentId, originalFileName)

    // -------------------------------
    // Helpers
    // -------------------------------
    private fun buildApiName(request: ApiKnowledgeRequestDto): String {
        val uri = URI(request.apiUrl)
        val path = uri.path.ifBlank { "/" }
        return "API: ${request.apiMethod.uppercase()} $path"
    }

    private fun uniquify(
        agentId: UUID,
        base: String,
    ): String {
        val existing = repo.findNamesByAgentIdAndNamePrefix(agentId, base)

        if (existing.isEmpty()) return base

        val suffixRegex = Regex("""\((\d+)\)$""")

        val maxIndex =
            existing
                .mapNotNull {
                    suffixRegex
                        .find(it)
                        ?.groupValues
                        ?.get(1)
                        ?.toInt()
                }.maxOrNull() ?: 0

        return "$base (${maxIndex + 1})"
    }

    private fun normalizeUrl(url: String): String =
        URI(url).let { uri ->
            buildString {
                append(uri.host)
                uri.path?.takeIf { it.isNotBlank() }?.let { append(it) }
            }
        }

    private fun extractHost(url: String): String = URI(url).host
}
