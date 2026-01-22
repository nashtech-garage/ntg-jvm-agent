package com.ntgjvmagent.orchestrator.embedding.queue

import com.ntgjvmagent.orchestrator.config.EmbeddingWorkerProperties
import com.ntgjvmagent.orchestrator.embedding.job.EmbeddingJobService
import com.ntgjvmagent.orchestrator.embedding.job.EmbeddingJobStatusService
import com.ntgjvmagent.orchestrator.embedding.runtime.EmbeddingService
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.EmbeddingJob
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.KnowledgeChunk
import com.ntgjvmagent.orchestrator.repository.KnowledgeChunkRepository
import com.ntgjvmagent.orchestrator.service.VectorStoreService
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
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

    @EventListener(ApplicationReadyEvent::class)
    fun startWorker() {
        log.info("Starting adaptive reactive embedding worker...")

        Flux
            .interval(Duration.ofMillis(props.pollIntervalMs))
            .flatMap { pollAndExecuteJobs() }
            .subscribe(
                { /* no-op */ },
                { ex -> log.error("Embedding worker crashed", ex) },
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
     *
     * Flow:
     *   1) Load chunk
     *   2) Embed content
     *   3) Write to vector store
     *   4) Mark job success
     */
    private fun processJobReactive(job: EmbeddingJob): Mono<Unit> {
        val agentId = job.agent.id!!
        val correlationId = "embed-${job.id}"

        return Mono
            .fromCallable {
                chunkRepo.findByIdOrNull(job.chunk.id!!)
                    ?: throw EntityNotFoundException("Chunk missing for job ${job.id}")
            }.subscribeOn(Schedulers.boundedElastic())
            .flatMap { chunk ->
                embeddingService
                    .embedReactive(
                        agentId = agentId,
                        text = chunk.content,
                        correlationId = "$correlationId:chunk-${chunk.id}",
                    ).map { embedding -> chunk to embedding }
            }.flatMap { (chunk, _) ->
                Mono
                    .fromCallable {
                        vectorStoreService
                            .getVectorStore()
                            .add(listOf(toDocument(chunk)))
                    }.subscribeOn(Schedulers.boundedElastic())
            }.then(
                Mono.defer {
                    statusService.markSuccess(job)
                    Mono.empty<Unit>()
                },
            ).onErrorResume { ex -> handleFailure(job, ex) }
    }

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
                    log.error("Job {} failed: {}", job.id, ex.message, ex)
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

    private fun toDocument(chunk: KnowledgeChunk): Document =
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
