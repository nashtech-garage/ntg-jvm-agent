package com.ntgjvmagent.authorizationserver.integration

import com.ntgjvmagent.authorizationserver.integration.config.PostgresTestContainer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = [PostgresTestContainer.Initializer::class])
class SecurityIT(
    @Autowired val mockMvc: MockMvc
) {

    @Test
    fun `should allow discovery document`() {
        mockMvc.get("/.well-known/oauth-authorization-server")
            .andExpect {
                status { isOk() }
            }
    }

    @Test
    fun `should protect userinfo endpoint`() {
        mockMvc.get("/userinfo")
            .andExpect {
                status { isUnauthorized() } // Requires token
            }
    }

    @Test
    fun `should redirect to login when accessing normal app endpoint`() {
        mockMvc.get("/")
            .andExpect {
                status { is3xxRedirection() }
            }
    }
}

