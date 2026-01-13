package com.ntgjvmagent.orchestrator.dto.request

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.ntgjvmagent.orchestrator.model.KnowledgeSourceType

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "sourceType",
)
@JsonSubTypes(
    JsonSubTypes.Type(UrlKnowledgeRequestDto::class, name = "WEB_URL"),
    JsonSubTypes.Type(SitemapKnowledgeRequestDto::class, name = "SITEMAP"),
    JsonSubTypes.Type(InlineKnowledgeRequestDto::class, name = "INLINE"),
    JsonSubTypes.Type(DatabaseKnowledgeRequestDto::class, name = "DATABASE"),
    JsonSubTypes.Type(ApiKnowledgeRequestDto::class, name = "API"),
)
sealed interface AgentKnowledgeRequestDto {
    val sourceType: KnowledgeSourceType
    val sourceUri: String?
    val metadata: Map<String, Any?>
}
