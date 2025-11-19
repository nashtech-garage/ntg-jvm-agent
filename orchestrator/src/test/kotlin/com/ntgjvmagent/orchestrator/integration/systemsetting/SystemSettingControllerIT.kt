package com.ntgjvmagent.orchestrator.integration.systemsetting

import com.ntgjvmagent.orchestrator.entity.SystemSettingEntity
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
        repository.save(
            SystemSettingEntity(
                siteName = "TEST",
                maximumUser = 10,
                sessionTimeout = 10,
                maximumSizeFileUpload = 10,
                allowedFileTypes = "doc, pdf",
                maintenanceMode = true,
                userRegistration = true,
                emailVerification = true,
            ),
        )

        mockMvc
            .perform(
                getAuth("/api/system", roles = listOf("ROLE_ADMIN")),
            ).andExpect(status().isOk)
            .andReturn()

    }
}
