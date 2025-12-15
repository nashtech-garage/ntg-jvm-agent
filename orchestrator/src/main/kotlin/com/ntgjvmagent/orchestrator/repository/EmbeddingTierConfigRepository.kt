package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.agent.knowledge.EmbeddingTierConfig
import org.springframework.data.jpa.repository.JpaRepository

interface EmbeddingTierConfigRepository : JpaRepository<EmbeddingTierConfig, Long>
