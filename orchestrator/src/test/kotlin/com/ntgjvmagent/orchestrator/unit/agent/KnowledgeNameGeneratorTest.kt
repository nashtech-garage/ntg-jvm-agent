package com.ntgjvmagent.orchestrator.unit.agent

import com.ntgjvmagent.orchestrator.component.KnowledgeNameGenerator
import com.ntgjvmagent.orchestrator.dto.request.ApiKnowledgeRequestDto
import com.ntgjvmagent.orchestrator.dto.request.DatabaseKnowledgeRequestDto
import com.ntgjvmagent.orchestrator.dto.request.SitemapKnowledgeRequestDto
import com.ntgjvmagent.orchestrator.dto.request.UrlKnowledgeRequestDto
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class KnowledgeNameGeneratorTest {
    private lateinit var repo: AgentKnowledgeRepository
    private lateinit var generator: KnowledgeNameGenerator

    private val agentId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        repo = mockk()
        generator = KnowledgeNameGenerator(repo)
    }

    // -------------------------------------------------
    // Base behavior
    // -------------------------------------------------

    @Test
    fun `returns base name when no collisions exist`() {
        every {
            repo.findNamesByAgentIdAndNamePrefix(agentId, "test.pdf")
        } returns emptyList()

        val result = generator.generateForFile(agentId, "test.pdf")

        assertEquals("test.pdf", result)
    }

    @Test
    fun `adds (1) when base already exists`() {
        every {
            repo.findNamesByAgentIdAndNamePrefix(agentId, "test.pdf")
        } returns listOf("test.pdf")

        val result = generator.generateForFile(agentId, "test.pdf")

        assertEquals("test.pdf (1)", result)
    }

    @Test
    fun `increments suffix based on highest existing index`() {
        every {
            repo.findNamesByAgentIdAndNamePrefix(agentId, "test.pdf")
        } returns
            listOf(
                "test.pdf",
                "test.pdf (1)",
                "test.pdf (2)",
                "test.pdf (10)",
            )

        val result = generator.generateForFile(agentId, "test.pdf")

        assertEquals("test.pdf (11)", result)
    }

    @Test
    fun `ignores names that do not end with numeric suffix`() {
        every {
            repo.findNamesByAgentIdAndNamePrefix(agentId, "test.pdf")
        } returns
            listOf(
                "test.pdf",
                "test.pdf (old)",
                "test.pdf (draft)",
                "test.pdf (2)", // only this should count
            )

        val result = generator.generateForFile(agentId, "test.pdf")

        assertEquals("test.pdf (3)", result)
    }

    // -------------------------------------------------
    // Prefix collision behavior (important edge case)
    // -------------------------------------------------

    @Test
    fun `file name prefix collisions still generate correct suffix`() {
        every {
            repo.findNamesByAgentIdAndNamePrefix(agentId, "file")
        } returns
            listOf(
                "file",
                "file (1)",
                "file-old",
                "file_backup",
            )

        val result = generator.generateForFile(agentId, "file")

        assertEquals("file (2)", result)
    }

    // -------------------------------------------------
    // URL normalization
    // -------------------------------------------------

    @Test
    fun `url knowledge name is normalized correctly`() {
        every {
            repo.findNamesByAgentIdAndNamePrefix(agentId, "example.com/path")
        } returns emptyList()

        val result =
            generator.generate(
                agentId,
                UrlKnowledgeRequestDto(
                    url = "https://example.com/path?x=1#fragment",
                ),
            )

        assertEquals("example.com/path", result)
    }

    // -------------------------------------------------
    // Sitemap naming
    // -------------------------------------------------

    @Test
    fun `sitemap knowledge name uses host only`() {
        every {
            repo.findNamesByAgentIdAndNamePrefix(agentId, "Sitemap: example.com")
        } returns emptyList()

        val result =
            generator.generate(
                agentId,
                SitemapKnowledgeRequestDto(
                    sitemapUrl = "https://example.com/sitemap.xml",
                ),
            )

        assertEquals("Sitemap: example.com", result)
    }

    // -------------------------------------------------
    // Database naming
    // -------------------------------------------------

    @Test
    fun `database knowledge name uses host and port`() {
        every {
            repo.findNamesByAgentIdAndNamePrefix(agentId, "DB: db.example.com:5432")
        } returns emptyList()

        val result =
            generator.generate(
                agentId,
                DatabaseKnowledgeRequestDto(
                    dbHost = "db.example.com",
                    dbPort = "5432",
                    dbUser = "user",
                    dbPassword = "pass",
                    dbQuery = "SELECT * FROM customers",
                ),
            )

        assertEquals("DB: db.example.com:5432", result)
    }

    // -------------------------------------------------
    // API naming
    // -------------------------------------------------

    @Test
    fun `api name includes method and path`() {
        every {
            repo.findNamesByAgentIdAndNamePrefix(agentId, "API: GET /v1/users")
        } returns emptyList()

        val result =
            generator.generate(
                agentId,
                ApiKnowledgeRequestDto(
                    apiUrl = "https://api.example.com/v1/users",
                    apiMethod = "get",
                ),
            )

        assertEquals("API: GET /v1/users", result)
    }
}
