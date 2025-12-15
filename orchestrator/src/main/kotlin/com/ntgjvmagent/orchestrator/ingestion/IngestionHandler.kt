package com.ntgjvmagent.orchestrator.ingestion

import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.model.KnowledgeSourceType

interface IngestionHandler {
    fun supports(type: KnowledgeSourceType): Boolean

    fun ingest(entity: AgentKnowledge)
}
