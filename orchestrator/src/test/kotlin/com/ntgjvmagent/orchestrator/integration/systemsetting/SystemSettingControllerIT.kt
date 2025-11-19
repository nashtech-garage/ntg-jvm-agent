package com.ntgjvmagent.orchestrator.integration.systemsetting

import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.repository.SystemSettingRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("SystemSettingController Integration Tests")
class SystemSettingControllerIT
    @Autowired
    constructor(
        private val repository: SystemSettingRepository,
    ) : BaseIntegrationTest() {
        @Test
    fun `should get SystemSetting`() {
        mockMvc
            .perform(
                getAuth("/api/system", roles = listOf("ROLE_ADMIN")),
            ).andExpect(status().isOk)
            .andReturn()
        }
    }
