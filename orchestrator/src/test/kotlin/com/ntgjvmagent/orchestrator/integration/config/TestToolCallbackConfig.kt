package com.ntgjvmagent.orchestrator.integration.config

import com.ntgjvmagent.orchestrator.component.FilteredToolCallbackProvider
import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TestToolCallbackConfig {
    @Bean
    fun filteredToolCallbackProvider(): FilteredToolCallbackProvider {
        val mock = Mockito.mock(FilteredToolCallbackProvider::class.java)
        Mockito
            .`when`(mock.getCallbacksByToolNames(Mockito.anyList()))
            .thenReturn(emptyList())
        return mock
    }
}
