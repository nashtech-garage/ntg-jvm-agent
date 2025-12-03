package com.ntgjvmagent.orchestrator.dto.request

import com.ntgjvmagent.orchestrator.model.KnowledgeSourceType
import jakarta.validation.constraints.NotBlank

data class SitemapKnowledgeRequestDto(
    @field:NotBlank
    override val name: String,
    @field:NotBlank
    val sitemapUrl: String,
) : AgentKnowledgeRequestDto {
    override val sourceType = KnowledgeSourceType.SITEMAP
    override val sourceUri: String = sitemapUrl
    override val metadata = mapOf("sitemapUrl" to sitemapUrl)
}
