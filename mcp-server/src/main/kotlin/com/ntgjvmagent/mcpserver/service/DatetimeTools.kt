package com.ntgjvmagent.mcpserver.service

import com.ntgjvmagent.mcpserver.utils.Constant
import org.springframework.ai.chat.messages.ToolResponseMessage
import org.springframework.ai.tool.annotation.Tool
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class DatetimeTools {
    @Tool(
        description = "Return current datetime in UTC format",
    )
    fun getCurrentDatetime(): ToolResponseMessage.ToolResponse {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        return ToolResponseMessage.ToolResponse(
            UUID.randomUUID().toString(),
            "Result of getCurrentDatetime tool",
            now.format(FORMATTER),
        )
    }

    companion object {
        private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(Constant.FULL_DATETIME_FORMAT)
    }
}
