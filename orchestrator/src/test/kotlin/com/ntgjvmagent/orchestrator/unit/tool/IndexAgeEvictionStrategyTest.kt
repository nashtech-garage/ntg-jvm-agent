package com.ntgjvmagent.orchestrator.unit.tool

import com.ntgjvmagent.orchestrator.tool.IndexAgeEvictionStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class IndexAgeEvictionStrategyTest {
    private val ttl = Duration.ofDays(7)
    private val start = Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun `a session whose own index aged out is evicted on its next access`() {
        val clock = MutableClock(start)
        val strategy = IndexAgeEvictionStrategy(ttl, clock)
        assertTrue(strategy.onAccess(SESSION).isEmpty())

        clock.advance(Duration.ofDays(8))

        // The library strategies never evict the session being accessed, which would leave a
        // conversation used once a week pointing at documents old enough to be swept.
        assertEquals(setOf(SESSION), strategy.onAccess(SESSION))
    }

    @Test
    fun `a session within the age bound is kept`() {
        val clock = MutableClock(start)
        val strategy = IndexAgeEvictionStrategy(ttl, clock)
        strategy.onAccess(SESSION)

        clock.advance(Duration.ofDays(6))

        assertTrue(strategy.onAccess(SESSION).isEmpty())
    }

    @Test
    fun `access does not extend the life of an index`() {
        val clock = MutableClock(start)
        val strategy = IndexAgeEvictionStrategy(ttl, clock)
        strategy.onAccess(SESSION)

        // Used every day: idleness-based eviction would never fire, but the documents behind
        // this session still age, so it has to be re-indexed once it reaches the bound.
        repeat(6) {
            clock.advance(Duration.ofDays(1))
            assertTrue(strategy.onAccess(SESSION).isEmpty())
        }
        clock.advance(Duration.ofDays(1))

        assertEquals(setOf(SESSION), strategy.onAccess(SESSION))
    }

    @Test
    fun `other expired sessions are evicted too`() {
        val clock = MutableClock(start)
        val strategy = IndexAgeEvictionStrategy(ttl, clock)
        strategy.onAccess("old-a")
        strategy.onAccess("old-b")

        clock.advance(Duration.ofDays(8))

        assertEquals(setOf("old-a", "old-b"), strategy.onAccess(SESSION))
    }

    @Test
    fun `onRemoved keeps the recorded index time`() {
        val clock = MutableClock(start)
        val strategy = IndexAgeEvictionStrategy(ttl, clock)
        strategy.onAccess(SESSION)

        // The advisor calls onRemoved while evicting the session it is about to re-index.
        // Forgetting the entry here would restart its clock on the following turn instead of at
        // the re-index, and the drift would accumulate across idle gaps.
        strategy.onRemoved(SESSION)
        clock.advance(Duration.ofDays(8))

        assertEquals(setOf(SESSION), strategy.onAccess(SESSION))
    }

    private class MutableClock(
        private var now: Instant,
    ) : Clock() {
        fun advance(amount: Duration) {
            now = now.plus(amount)
        }

        override fun instant(): Instant = now

        override fun getZone() = ZoneOffset.UTC

        override fun withZone(zone: java.time.ZoneId): Clock = this
    }

    private companion object {
        const val SESSION = "session-a"
    }
}
