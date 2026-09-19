package com.ntgjvmagent.mcpserver.service

import org.springframework.ai.mcp.annotation.McpTool
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class DatetimeTool(
    private val clock: Clock,
) {
    @McpTool(
        name = "getCurrentDatetime",
        title = "Current UTC datetime",
        description = "Return the current UTC datetime as an ISO-8601 timestamp",
        annotations =
            McpTool.McpAnnotations(
                readOnlyHint = true,
                destructiveHint = false,
                idempotentHint = false,
                openWorldHint = false,
            ),
    )
    fun getCurrentDatetime(): CurrentDatetimeResponse =
        CurrentDatetimeResponse(
            datetimeUtc = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS).toString(),
        )
}

data class CurrentDatetimeResponse(
    val datetimeUtc: String,
)
