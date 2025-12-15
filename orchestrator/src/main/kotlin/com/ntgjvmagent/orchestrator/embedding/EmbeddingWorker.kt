package com.ntgjvmagent.orchestrator.embedding

import com.ntgjvmagent.orchestrator.config.EmbeddingWorkerProperties
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.EmbeddingJob
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.KnowledgeChunk
import com.ntgjvmagent.orchestrator.repository.KnowledgeChunkRepository
import com.ntgjvmagent.orchestrator.service.VectorStoreService
import com.ntgjvmagent.orchestrator.utils.Constant
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import jakarta.annotation.PostConstruct
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.dao.DataAccessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Duration

@Service
class EmbeddingWorker(
    private val embeddingJobService: EmbeddingJobService,
    private val chunkRepo: KnowledgeChunkRepository,
    private val embeddingService: EmbeddingService,
    private val vectorStoreService: VectorStoreService,
    private val statusService: EmbeddingJobStatusService,
    private val props: EmbeddingWorkerProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Volatile
    private var currentConcurrency = props.minConcurrency

    /** Set when we receive a RequestNotPermitted so concurrency will not increase */
    @Volatile
    private var rateLimitedRecently = false

    @PostConstruct
    fun startWorker() {
        log.info("Starting adaptive reactive embedding worker...")

        Flux
            .interval(Duration.ofMillis(props.pollIntervalMs))
            .flatMap { pollAndExecuteJobs() }
            .subscribe(
                { /* no-op */ },
                { ex -> log.error("Worker crashed: {}", ex.message, ex) },
            )
    }

    /**
     * Poll up to `currentConcurrency` jobs and process them reactively.
     */
    private fun pollAndExecuteJobs(): Flux<Unit> {
        val jobs = embeddingJobService.takeBatch(currentConcurrency)

        if (jobs.isEmpty()) {
            decreaseConcurrency()
            return Flux.empty()
        }

        increaseConcurrency(jobs.size)

        return Flux
            .fromIterable(jobs)
            .flatMap({ job -> processJobReactive(job) }, currentConcurrency)
    }

    /**
     * Full reactive job execution.
     */
    private fun processJobReactive(job: EmbeddingJob): Mono<Unit> {
        val agentId = job.agent.id!!

        return Mono
            .fromCallable {
                chunkRepo.findByIdOrNull(job.chunk.id!!)
                    ?: throw EntityNotFoundException("Chunk missing for job ${job.id}")
            }.subscribeOn(Schedulers.boundedElastic())
            .flatMap { chunk ->
                embeddingService
                    .embedReactive(agentId, chunk.content)
                    .map { embedding -> chunk to embedding }
            }.flatMap { (chunk, embedding) ->
                persistEmbeddingReactive(chunk, embedding)
            }.flatMap { chunk ->
                val docs = listOf(toDocument(chunk))

                Mono
                    .fromCallable {
                        vectorStoreService.getVectorStore(agentId).add(docs)
                    }.subscribeOn(Schedulers.boundedElastic())
                    .thenReturn(chunk)
            }.then(
                Mono.defer {
                    statusService.markSuccess(job)
                    Mono.empty<Unit>()
                },
            ).onErrorResume { ex -> handleFailure(job, ex) }
    }

    /**
     * Persist embeddings in DB (reactively via boundedElastic).
     */
    private fun persistEmbeddingReactive(
        chunk: KnowledgeChunk,
        embedding: FloatArray,
    ): Mono<KnowledgeChunk> =
        Mono
            .fromCallable {
                when (embedding.size) {
                    Constant.GEMINI_DIMENSION -> chunk.embedding768 = embedding
                    Constant.CHATGPT_DIMENSION -> chunk.embedding1536 = embedding
                    else -> throw IllegalArgumentException("Unsupported embedding dimension ${embedding.size}")
                }
                chunkRepo.save(chunk)
            }.subscribeOn(Schedulers.boundedElastic())

    /**
     * Worker-level failure handler.
     */
    private fun handleFailure(
        job: EmbeddingJob,
        ex: Throwable,
    ): Mono<Unit> =
        Mono.defer {
            when (ex) {
                is RequestNotPermitted -> {
                    log.warn("RateLimiter blocked job {}. Retrying later.", job.id)
                    rateLimitedRecently = true
                    statusService.markRetryLater(job, ex)
                }

                is DataAccessException -> {
                    log.warn("DB failure for job {}: {}. Retrying later.", job.id, ex.message)
                    statusService.markRetryLater(job, ex)
                }

                is EntityNotFoundException -> {
                    log.error("Missing chunk for job {}: {}", job.id, ex.message)
                    statusService.markFailure(job, ex)
                }

                else -> {
                    log.error("Job {} failed after embedding retries: {}", job.id, ex.message)
                    statusService.markFailure(job, ex)
                }
            }
            Mono.empty()
        }

    // ---------------------------------------------------------------------
    // Adaptive concurrency logic
    // ---------------------------------------------------------------------

    private fun increaseConcurrency(jobsReceived: Int) {
        if (!rateLimitedRecently &&
            currentConcurrency < props.maxConcurrency &&
            jobsReceived == currentConcurrency
        ) {
            currentConcurrency += props.stepUp
            log.info("Increasing worker concurrency to {}", currentConcurrency)
        }
    }

    private fun decreaseConcurrency() {
        if (currentConcurrency > props.minConcurrency) {
            currentConcurrency -= props.stepDown
            log.info("Decreasing worker concurrency to {}", currentConcurrency)
        }

        // Reset rate limit flag once we reduce concurrency
        rateLimitedRecently = false
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
