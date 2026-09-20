package com.ntgjvmagent.orchestrator.unit.config

import com.ntgjvmagent.orchestrator.config.ToolSearchRoutingProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToolSearchRoutingPropertiesTest {
    @Test
    fun `uses direct tool calling below the catalog threshold`() {
        val properties = ToolSearchRoutingProperties(minCatalogSize = 10)

        assertFalse(properties.shouldUseToolSearch(0))
        assertFalse(properties.shouldUseToolSearch(9))
    }

    @Test
    fun `uses progressive discovery at the catalog threshold`() {
        val properties = ToolSearchRoutingProperties(minCatalogSize = 10)

        assertTrue(properties.shouldUseToolSearch(10))
        assertTrue(properties.shouldUseToolSearch(25))
    }

    @Test
    fun `rejects invalid catalog sizes`() {
        assertThrows<IllegalArgumentException> { ToolSearchRoutingProperties(minCatalogSize = 0) }
        assertThrows<IllegalArgumentException> { ToolSearchRoutingProperties().shouldUseToolSearch(-1) }
    }
}
