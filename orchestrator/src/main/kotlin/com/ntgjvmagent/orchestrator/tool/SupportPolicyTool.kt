package com.ntgjvmagent.orchestrator.tool

import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Component

@Component
class SupportPolicyTool {
    companion object {
        const val TOOL_NAME = "getSupportResponseTarget"
        const val TOOL_DESCRIPTION =
            "Look up the initial response target in the NTG support policy for a customer tier and incident priority."

        private val RESPONSE_TARGETS =
            mapOf(
                CustomerTier.STANDARD to
                    mapOf(
                        IncidentPriority.P1 to 30,
                        IncidentPriority.P2 to 120,
                        IncidentPriority.P3 to 480,
                    ),
                CustomerTier.PREMIUM to
                    mapOf(
                        IncidentPriority.P1 to 15,
                        IncidentPriority.P2 to 60,
                        IncidentPriority.P3 to 240,
                    ),
            )
    }

    @Tool(name = TOOL_NAME, description = TOOL_DESCRIPTION)
    fun getSupportResponseTarget(
        @ToolParam(description = "Customer support tier: STANDARD or PREMIUM.")
        customerTier: CustomerTier,
        @ToolParam(description = "Incident priority: P1, P2, or P3.")
        incidentPriority: IncidentPriority,
    ): SupportResponseTarget =
        SupportResponseTarget(
            customerTier = customerTier,
            incidentPriority = incidentPriority,
            initialResponseMinutes = RESPONSE_TARGETS.getValue(customerTier).getValue(incidentPriority),
        )

    enum class CustomerTier {
        STANDARD,
        PREMIUM,
    }

    enum class IncidentPriority {
        P1,
        P2,
        P3,
    }

    data class SupportResponseTarget(
        val customerTier: CustomerTier,
        val incidentPriority: IncidentPriority,
        val initialResponseMinutes: Int,
    )
}
