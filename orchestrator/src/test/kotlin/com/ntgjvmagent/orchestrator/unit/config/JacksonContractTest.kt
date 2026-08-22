package com.ntgjvmagent.orchestrator.unit.config

import com.ntgjvmagent.orchestrator.config.JacksonConfig
import com.ntgjvmagent.orchestrator.dto.request.AuthenticationRequestDto
import com.ntgjvmagent.orchestrator.dto.request.ToolRequestDto
import com.ntgjvmagent.orchestrator.utils.AuthType
import kotlin.test.Test
import kotlin.test.assertEquals

class JacksonContractTest {
    private val mapper = JacksonConfig().objectMapper()

    @Test
    fun `tool connection JSON shape remains stable`() {
        val request =
            ToolRequestDto(
                baseUrl = "http://localhost:9003",
                transportType = "STREAMABLE",
                endpoint = "/mcp",
                authorization = AuthenticationRequestDto(AuthType.BEARER, "secret", null),
            )

        assertEquals(
            "{\"baseUrl\":\"http://localhost:9003\",\"transportType\":\"STREAMABLE\",\"endpoint\":\"/mcp\",\"authorization\":{\"type\":\"BEARER\",\"token\":\"secret\",\"headerName\":null}}",
            mapper.writeValueAsString(request),
        )
    }
}
