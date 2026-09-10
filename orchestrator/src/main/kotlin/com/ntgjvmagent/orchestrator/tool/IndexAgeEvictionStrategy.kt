package com.ntgjvmagent.orchestrator.tool

import org.springframework.ai.tool.toolsearch.eviction.ToolIndexEvictionStrategy
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Evicts a session's tool index once it has been in place for [maxIndexAge], so that a long-lived
 * session is periodically re-indexed instead of relying on documents written once and never
 * touched again.
 *
 * The library's own strategies measure idleness ([org.springframework.ai.tool.toolsearch.eviction.TtlEvictionStrategy])
 * or count sessions ([org.springframework.ai.tool.toolsearch.eviction.LruEvictionStrategy]). Neither bounds how old the
 * documents behind a still-used session may get, and the advisor only re-indexes when it evicts
 * or when the tool set changes -- so a conversation used every day would keep pointing at
 * documents written on its first turn, which ToolIndexCleanupJob would eventually sweep out from
 * under it.
 *
 * Unlike the library strategies, the session being accessed is evicted too when its own entry has
 * aged out; the advisor drops its fingerprint on eviction and re-indexes before serving the turn.
 */
class IndexAgeEvictionStrategy(
    private val maxIndexAge: Duration,
    private val clock: Clock = Clock.systemUTC(),
) : ToolIndexEvictionStrategy {
    private val indexedAt = ConcurrentHashMap<String, Instant>()

    override fun onAccess(sessionId: String): Set<String> {
        val now = clock.instant()
        val expired =
            indexedAt
                .filterValues { Duration.between(it, now) >= maxIndexAge }
                .keys
                .toSet()

        expired.forEach(indexedAt::remove)
        // Records when this session's documents are current as of, which is now for a session
        // that was just expired (the advisor re-indexes it on this very turn) and unchanged for
        // one that was already tracked.
        indexedAt.putIfAbsent(sessionId, now)
        return expired
    }

    /**
     * Deliberately a no-op.
     *
     * The advisor calls this while evicting, including for the session it is about to re-index.
     * Dropping the entry that [onAccess] just recorded would restart this session's clock on the
     * following turn instead of at the re-index, and the drift would accumulate across idle gaps
     * until the tracked age no longer reflected how old the documents really are. An entry left
     * behind by an eviction this strategy did not ask for costs one extra re-index later.
     */
    override fun onRemoved(sessionId: String) = Unit
}
