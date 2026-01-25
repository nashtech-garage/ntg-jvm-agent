package com.ntgjvmagent.orchestrator.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.ntgjvmagent.orchestrator.entity.User
import com.ntgjvmagent.orchestrator.integration.config.AsyncTestConfig
import com.ntgjvmagent.orchestrator.integration.config.DisableSchedulingConfig
import com.ntgjvmagent.orchestrator.integration.config.DisableVectorStoreInitializerConfig
import com.ntgjvmagent.orchestrator.integration.config.PostgresTestContainer
import com.ntgjvmagent.orchestrator.integration.config.TestAuditorConfig
import com.ntgjvmagent.orchestrator.integration.config.TestEmbeddingConfig
import com.ntgjvmagent.orchestrator.repository.UserRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestInstance
import org.springframework.ai.mcp.client.common.autoconfigure.McpClientAutoConfiguration
import org.springframework.ai.mcp.client.common.autoconfigure.McpToolCallbackAutoConfiguration
import org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import reactor.core.scheduler.Schedulers

@ActiveProfiles("test")
@SpringBootTest
@ImportAutoConfiguration(
    exclude = [
        McpClientAutoConfiguration::class,
        McpToolCallbackAutoConfiguration::class,
        ToolCallingAutoConfiguration::class,
    ],
)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Container spins up once per class
@ContextConfiguration(initializers = [PostgresTestContainer.Initializer::class])
@Import(
    TestEmbeddingConfig::class,
    TestAuditorConfig::class,
    AsyncTestConfig::class,
    DisableSchedulingConfig::class,
    DisableVectorStoreInitializerConfig::class,
)
@Tag("integration")
abstract class BaseIntegrationTest {
    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var userRepository: UserRepository

    @BeforeAll
    fun setupTestUser() {
        if (userRepository.findByIdOrNull(TestAuditorConfig.TEST_USER_ID) == null) {
            userRepository.save(
                User(
                    id = TestAuditorConfig.TEST_USER_ID,
                    username = "integrationtestuser",
                    password = "password",
                    name = "Integration Test User",
                    email = "integrationtestuser@testemail.com",
                ),
            )
        }
    }

    @AfterAll
    fun shutdownSchedulers() {
        Schedulers.shutdownNow() // clean Reactor threads

        Thread.getAllStackTraces().forEach { (thread, stack) ->
            if (!thread.isDaemon) {
                println("NON-DAEMON THREAD STILL RUNNING: ${thread.name}")
                stack.forEach { element ->
                    println("    at $element")
                }
            }
        }
    }

    protected fun asJson(obj: Any): String = objectMapper.writeValueAsString(obj)

    /**
     * Helper: build JWT with custom roles and scopes.
     * Roles must NOT include "ROLE_" prefix; scopes must NOT include "SCOPE_" prefix.
     */
    protected fun jwtWith(
        roles: List<String> = emptyList(),
        scopes: List<String> = emptyList(),
    ) = jwt().authorities(
        roles.map { SimpleGrantedAuthority(it) } +
            scopes.map { SimpleGrantedAuthority(it) },
    )

    /**
     * Apply JWT with roles & scopes to any request builder
     */
    protected fun MockHttpServletRequestBuilder.withAuth(
        roles: List<String> = emptyList(),
        scopes: List<String> = emptyList(),
    ) = this.with(jwtWith(roles, scopes))

    /**
     * Convenience methods for HTTP verbs with auth
     */
    protected fun getAuth(
        url: String,
        roles: List<String> = emptyList(),
        scopes: List<String> = emptyList(),
    ) = get(url).withAuth(roles, scopes)

    protected fun postAuth(
        url: String,
        body: Any,
        roles: List<String> = emptyList(),
        scopes: List<String> = emptyList(),
    ) = post(url)
        .withAuth(roles, scopes)
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJson(body))

    protected fun putAuth(
        url: String,
        body: Any,
        roles: List<String> = emptyList(),
        scopes: List<String> = emptyList(),
    ) = put(url)
        .withAuth(roles, scopes)
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJson(body))

    protected fun deleteAuth(
        url: String,
        roles: List<String> = emptyList(),
        scopes: List<String> = emptyList(),
    ) = delete(url).withAuth(roles, scopes)

    /**
     * Convenience for multipart/form-data requests with auth
     */
    protected fun multipartAuth(
        url: String,
        file: MockMultipartFile,
        roles: List<String> = emptyList(),
        scopes: List<String> = emptyList(),
    ) = multipart(url)
        .file(file)
        .withAuth(roles, scopes)
        .contentType(MediaType.MULTIPART_FORM_DATA)

    protected fun <T : AbstractMockHttpServletRequestBuilder<T>> T.withAuth(
        roles: List<String> = emptyList(),
        scopes: List<String> = emptyList(),
    ): T = this.with(jwtWith(roles, scopes))
}
