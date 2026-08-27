package com.ntgjvmagent.mcpserver.service

import org.springframework.ai.tool.annotation.Tool
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class DatetimeTool(
    private val clock: Clock,
) {
    @Tool(
        description = "Return the current UTC datetime as an ISO-8601 timestamp",
    )
    fun getCurrentDatetime(): CurrentDatetimeResponse =
        CurrentDatetimeResponse(
            datetimeUtc = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS).toString(),
        )
}

data class CurrentDatetimeResponse(
    val datetimeUtc: String,
)
