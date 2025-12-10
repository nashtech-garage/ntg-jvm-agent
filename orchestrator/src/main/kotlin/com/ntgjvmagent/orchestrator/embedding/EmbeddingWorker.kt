package com.ntgjvmagent.orchestrator.embedding

import com.ntgjvmagent.orchestrator.entity.agent.knowledge.EmbeddingJob
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.KnowledgeChunk
import com.ntgjvmagent.orchestrator.repository.KnowledgeChunkRepository
import com.ntgjvmagent.orchestrator.service.VectorStoreService
import com.ntgjvmagent.orchestrator.utils.Constant
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.dao.DataAccessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpClientErrorException

@Service
class EmbeddingWorker(
    private val embeddingJobService: EmbeddingJobService,
    private val chunkRepo: KnowledgeChunkRepository,
    private val embeddingService: EmbeddingService,
    private val vectorStoreService: VectorStoreService,
    private val statusService: EmbeddingJobStatusService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 1000)
    fun processQueue() {
        val job = embeddingJobService.takeJob() ?: return
        safeExecute(job)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun safeExecute(job: EmbeddingJob) {
        try {
            executeJob(job)
        } catch (ex: RequestNotPermitted) {
            log.warn("Rate limit hit for job {}. Delaying retry...", job.id)
            statusService.markRetryLater(job, ex)
        } catch (ex: HttpClientErrorException.TooManyRequests) {
            log.warn("429 for job {}. Delaying retry...", job.id)
            statusService.markRetryLater(job, ex)
        } catch (ex: DataAccessException) {
            log.error("DB failure for job {}: {}", job.id, ex.message)
            statusService.markRetryLater(job, ex)
        } catch (ex: Exception) {
            log.error("Unexpected error for job {}: {}", job.id, ex.message, ex)
            statusService.markFailure(job, ex)
        }
    }

    fun executeJob(job: EmbeddingJob) {
        val chunk =
            chunkRepo.findByIdOrNull(job.chunk.id!!)
                ?: throw EntityNotFoundException("Chunk missing")

        val agentId = job.agent.id!!

        val embedding = embeddingService.embed(agentId, chunk.content)

        persistEmbeddingTx(chunk, embedding)

        vectorStoreService
            .getVectorStore(agentId)
            .add(listOf(toDocument(chunk)))

        statusService.markSuccess(job)
    }

    @Transactional
    fun persistEmbeddingTx(
        chunk: KnowledgeChunk,
        embedding: FloatArray,
    ) {
        when (embedding.size) {
            Constant.GEMINI_DIMENSION -> chunk.embedding768 = embedding
            Constant.CHATGPT_DIMENSION -> chunk.embedding1536 = embedding
            else -> throw IllegalArgumentException("Unsupported dimension")
        }
        chunkRepo.save(chunk)
    }

    private fun toDocument(chunk: KnowledgeChunk) =
        Document(
            chunk.id.toString(),
            chunk.content,
            chunk.metadata +
                mapOf(
                    "chunkId" to chunk.id.toString(),
                    "chunkOrder" to chunk.chunkOrder,
                    "knowledgeId" to chunk.knowledge.id.toString(),
                ),
        )
}
