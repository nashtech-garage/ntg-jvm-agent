package com.ntgjvmagent.orchestrator.unit.tool

import com.ntgjvmagent.orchestrator.tool.LocalToolCatalog
import com.ntgjvmagent.orchestrator.tool.SupportPolicyTool
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SupportPolicyToolTest {
    private val tool = SupportPolicyTool()

    @Test
    fun `support response matrix is deterministic`() {
        val expectedTargets =
            mapOf(
                SupportPolicyTool.CustomerTier.STANDARD to listOf(30, 120, 480),
                SupportPolicyTool.CustomerTier.PREMIUM to listOf(15, 60, 240),
            )

        expectedTargets.forEach { (tier, targets) ->
            SupportPolicyTool.IncidentPriority.entries.forEachIndexed { index, priority ->
                assertEquals(
                    targets[index],
                    tool.getSupportResponseTarget(tier, priority).initialResponseMinutes,
                )
            }
        }
    }

    @Test
    fun `tool callback exposes the exact typed schema and canonical result`() {
        val callback = LocalToolCatalog(tool).getToolCallbacks().single()
        val definition = callback.toolDefinition
        val schema = JsonMapper.builder().build().readTree(definition.inputSchema())

        assertEquals(SupportPolicyTool.TOOL_NAME, definition.name())
        assertEquals(SupportPolicyTool.TOOL_DESCRIPTION, definition.description())
        assertEquals(setOf("customerTier", "incidentPriority"), schema["properties"].propertyNames().toSet())
        assertTrue(
            schema["required"].values().map { it.asString() }.toSet() ==
                setOf("customerTier", "incidentPriority"),
        )
        assertTrue(
            schema["properties"]["customerTier"]["enum"].values().map { it.asString() }.toSet() ==
                setOf("STANDARD", "PREMIUM"),
        )
        assertTrue(
            schema["properties"]["incidentPriority"]["enum"].values().map { it.asString() }.toSet() ==
                setOf("P1", "P2", "P3"),
        )
        assertEquals(false, schema["additionalProperties"].asBoolean())

        val result =
            callback.call(
                """
                {
                  "customerTier": "PREMIUM",
                  "incidentPriority": "P1"
                }
                """.trimIndent(),
            )

        assertTrue(result.contains("\"customerTier\":\"PREMIUM\""))
        assertTrue(result.contains("\"incidentPriority\":\"P1\""))
        assertTrue(result.contains("\"initialResponseMinutes\":15"))
    }
}
