package com.ntgjvmagent.orchestrator.advisor

import com.ntgjvmagent.orchestrator.component.AgentChatClientFactory
import com.ntgjvmagent.orchestrator.config.SessionCompactionProperties
import com.ntgjvmagent.orchestrator.token.accounting.TokenAccountingFacade
import com.ntgjvmagent.orchestrator.token.estimation.SpringAiTokenCountEstimatorAdapter
import com.ntgjvmagent.orchestrator.token.estimation.TokenEstimatorSelector
import org.springframework.ai.session.compaction.CompactionStrategy
import org.springframework.ai.session.compaction.CompactionTrigger
import org.springframework.ai.session.compaction.CompositeCompactionTrigger
import org.springframework.ai.session.compaction.RecursiveSummarizationCompactionStrategy
import org.springframework.ai.session.compaction.TokenCountTrigger
import org.springframework.ai.session.compaction.TurnCountTrigger
import java.util.UUID

class SessionCompactionFactory(
    private val chatClientFactory: AgentChatClientFactory,
    private val tokenFacade: TokenAccountingFacade,
    private val tokenEstimatorSelector: TokenEstimatorSelector,
    private val properties: SessionCompactionProperties,
) {
    fun create(
        agentId: UUID,
        userId: UUID,
        model: String,
        settings: Map<String, Any>?,
        rootCorrelationId: String,
    ): SessionCompaction? {
        val compaction = properties.resolve(settings)
        if (!compaction.enabled) {
            return null
        }

        val estimator = SpringAiTokenCountEstimatorAdapter(model, tokenEstimatorSelector)
        val accountingAdvisor =
            CompactionAccountingAdvisor(
                userId = userId,
                agentId = agentId,
                model = model,
                rootCorrelationId = rootCorrelationId,
                tokenFacade = tokenFacade,
                tokenCountEstimator = estimator,
            )
        val compactionClient =
            chatClientFactory
                .createWithoutToolSearch(agentId)
                .mutate()
                .defaultAdvisors(accountingAdvisor)
                .build()
        val trigger =
            CompositeCompactionTrigger.anyOf(
                TurnCountTrigger(compaction.turnThreshold),
                TokenCountTrigger
                    .builder()
                    .threshold(compaction.tokenThreshold)
                    .tokenCountEstimator(estimator)
                    .build(),
            )
        val strategy =
            RecursiveSummarizationCompactionStrategy
                .builder(compactionClient)
                .maxEventsToKeep(compaction.maxEventsToKeep)
                .overlapSize(compaction.overlapSize)
                .tokenCountEstimator(estimator)
                .build()

        return SessionCompaction(
            trigger = FailSafeCompactionTrigger(trigger),
            strategy = FailSafeCompactionStrategy(strategy),
        )
    }
}

data class SessionCompaction(
    val trigger: CompactionTrigger,
    val strategy: CompactionStrategy,
)
