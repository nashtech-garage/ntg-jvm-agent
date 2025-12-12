package com.ntgjvmagent.orchestrator.embedding

import com.ntgjvmagent.orchestrator.entity.agent.knowledge.EmbeddingJob
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.KnowledgeChunk
import com.ntgjvmagent.orchestrator.repository.EmbeddingJobRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EmbeddingQueueService(
    private val embeddingJobRepo: EmbeddingJobRepository,
) {
    @Transactional
    fun enqueueForChunk(chunk: KnowledgeChunk): EmbeddingJob {
        val job =
            EmbeddingJob(
                agent = chunk.knowledge.agent,
                knowledge = chunk.knowledge,
                chunk = chunk,
            )

        return embeddingJobRepo.save(job)
    }
}
