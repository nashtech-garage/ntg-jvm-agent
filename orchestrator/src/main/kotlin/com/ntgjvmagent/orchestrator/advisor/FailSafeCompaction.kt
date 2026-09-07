package com.ntgjvmagent.orchestrator.advisor

import org.slf4j.LoggerFactory
import org.springframework.ai.session.compaction.CompactionRequest
import org.springframework.ai.session.compaction.CompactionResult
import org.springframework.ai.session.compaction.CompactionStrategy
import org.springframework.ai.session.compaction.CompactionTrigger

/**
 * Keeps a failing compaction from failing the chat request it rides on.
 *
 * `SessionMemoryAdvisor.after()` calls `SessionService.compact(...)` inline in the response
 * pipeline, and the library only handles a blank summary gracefully -- every exception
 * propagates. Downstream, `ConversationStreamingService` composes the stream as
 * `stream.concatWith(completion)`, so an error raised after the last chunk skips `completion`
 * entirely: the user receives the full answer text followed by an error event, and the turn is
 * never written to `chat_message`.
 *
 * Compaction is a background optimisation of the event log. Losing it costs context window,
 * never the correctness of an answer that was already produced, so it degrades quietly: log and
 * leave the event log untouched -- exactly what the library itself returns when the trigger
 * declines.
 */
class FailSafeCompactionTrigger(
    val delegate: CompactionTrigger,
) : CompactionTrigger {
    // Catching broadly is the point: this is a bulkhead, and the summarization client can fail
    // in any number of ways that must not reach the caller.
    @Suppress("TooGenericExceptionCaught")
    override fun shouldCompact(request: CompactionRequest): Boolean =
        try {
            delegate.shouldCompact(request)
        } catch (ex: Exception) {
            logger.warn("Compaction trigger failed for session {}; skipping compaction", request.session().id(), ex)
            false
        }

    companion object {
        private val logger = LoggerFactory.getLogger(FailSafeCompactionTrigger::class.java)
    }
}

/** @see FailSafeCompactionTrigger */
class FailSafeCompactionStrategy(
    val delegate: CompactionStrategy,
) : CompactionStrategy {
    @Suppress("TooGenericExceptionCaught")
    override fun compact(request: CompactionRequest): CompactionResult =
        try {
            delegate.compact(request)
        } catch (ex: Exception) {
            logger.warn("Compaction failed for session {}; event log left unchanged", request.session().id(), ex)
            CompactionResult(request.events(), emptyList(), 0)
        }

    companion object {
        private val logger = LoggerFactory.getLogger(FailSafeCompactionStrategy::class.java)
    }
}
