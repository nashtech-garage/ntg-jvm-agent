package com.ntgjvmagent.mcpserver.config

import com.ntgjvmagent.mcpserver.service.DatetimeTools
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class McpToolConfig {
    @Bean
    fun toolProvider(datetimeTool: DatetimeTools): ToolCallbackProvider =
        MethodToolCallbackProvider
            .builder()
            .toolObjects(datetimeTool)
            .build()
}
