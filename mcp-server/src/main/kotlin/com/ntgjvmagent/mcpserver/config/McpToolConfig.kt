package com.ntgjvmagent.mcpserver.config

import com.ntgjvmagent.mcpserver.service.DatetimeTool
import com.ntgjvmagent.mcpserver.service.SearchOnlineTool
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.time.Clock

@Configuration
class McpToolConfig {
    @Bean
    fun toolProvider(
        datetimeTool: DatetimeTool,
        searchOnlineTool: SearchOnlineTool,
    ): ToolCallbackProvider =
        MethodToolCallbackProvider
            .builder()
            .toolObjects(datetimeTool, searchOnlineTool)
            .build()

    @Bean
    fun restTemplate(): RestTemplate = RestTemplate()

    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
