package com.ntgjvmagent.orchestrator.unit.config

import com.ntgjvmagent.orchestrator.config.SessionCompactionProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SessionCompactionPropertiesTest {
    @Test
    fun `agent settings override every compaction option`() {
        val resolved =
            SessionCompactionProperties().resolve(
                mapOf(
                    "sessionCompaction" to
                        mapOf(
                            "enabled" to true,
                            "turnThreshold" to 7L,
                            "tokenThreshold" to "3200",
                            "maxEventsToKeep" to 5,
                            "overlapSize" to 1,
                        ),
                ),
            )

        assertEquals(true, resolved.enabled)
        assertEquals(7, resolved.turnThreshold)
        assertEquals(3_200, resolved.tokenThreshold)
        assertEquals(5, resolved.maxEventsToKeep)
        assertEquals(1, resolved.overlapSize)
    }

    @Test
    fun `invalid strategy window is rejected`() {
        assertThrows<IllegalArgumentException> {
            SessionCompactionProperties(maxEventsToKeep = 2, overlapSize = 2).resolve(null)
        }
    }
}
