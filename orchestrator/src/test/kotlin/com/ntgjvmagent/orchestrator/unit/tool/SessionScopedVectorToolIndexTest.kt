package com.ntgjvmagent.orchestrator.unit.tool

import com.ntgjvmagent.orchestrator.tool.SessionScopedVectorToolIndex
import com.ntgjvmagent.orchestrator.unit.support.BagOfWordsEmbeddingModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.ai.document.Document
import org.springframework.ai.tool.toolsearch.ToolReference
import org.springframework.ai.tool.toolsearch.ToolSearchRequest
import org.springframework.ai.vectorstore.SimpleVectorStore
import org.springframework.ai.vectorstore.VectorStore

/**
 * Exercises the index against a real [SimpleVectorStore] rather than a mock, so the session
 * metadata filter is actually evaluated. Session scoping is the whole guarantee of this class:
 * one session must never see a tool indexed for another.
 */
class SessionScopedVectorToolIndexTest {
    @Test
    fun `a session only ever sees its own tools`() {
        val store = store()
        val index = SessionScopedVectorToolIndex(store)
        index.indexTools(SESSION_A, listOf(tool("refundOrder", "Refund a customer order")))
        index.indexTools(SESSION_B, listOf(tool("refundInvoice", "Refund an invoice")))

        // A query that matches session B's tool at least as well as session A's.
        val results = index.search(ToolSearchRequest(SESSION_A, "refund", 5, null)).toolReferences()

        assertEquals(listOf("refundOrder"), results.map { it.toolName() })
    }

    @Test
    fun `clearIndex removes only the tools of that session`() {
        val store = store()
        val index = SessionScopedVectorToolIndex(store)
        index.indexTools(SESSION_A, listOf(tool("refundOrder", "Refund a customer order")))
        index.indexTools(SESSION_B, listOf(tool("refundInvoice", "Refund an invoice")))

        index.clearIndex(SESSION_A)

        assertTrue(index.search(ToolSearchRequest(SESSION_A, "refund", 5, null)).toolReferences().isEmpty())
        assertEquals(
            listOf("refundInvoice"),
            index.search(ToolSearchRequest(SESSION_B, "refund", 5, null)).toolReferences().map { it.toolName() },
        )
    }

    @Test
    fun `search round-trips name, summary and score`() {
        val index = SessionScopedVectorToolIndex(store())
        index.indexTool(SESSION_A, tool("refundOrder", "Refund a customer order"))

        val response = index.search(ToolSearchRequest(SESSION_A, "refund a customer order", 5, null))
        val reference = response.toolReferences().single()

        assertEquals("refundOrder", reference.toolName())
        assertEquals("Refund a customer order", reference.summary())
        assertNotNull(reference.relevanceScore())
        assertEquals(1, response.totalMatches())
    }

    @Test
    fun `maxResults caps the number of tools returned`() {
        val index = SessionScopedVectorToolIndex(store())
        index.indexTools(
            SESSION_A,
            (1..5).map { tool("refundOrder$it", "Refund a customer order variant $it") },
        )

        val results = index.search(ToolSearchRequest(SESSION_A, "refund", 2, null)).toolReferences()

        assertEquals(2, results.size)
    }

    @Test
    fun `indexing nothing touches nothing`() {
        val store = store()
        SessionScopedVectorToolIndex(store).indexTools(SESSION_A, emptyList())

        assertTrue(
            SessionScopedVectorToolIndex(store)
                .search(ToolSearchRequest(SESSION_A, "refund", 5, null))
                .toolReferences()
                .isEmpty(),
        )
    }

    private fun tool(
        name: String,
        summary: String,
    ): ToolReference =
        ToolReference
            .builder()
            .toolName(name)
            .summary(summary)
            .build()

    private fun store(): VectorStore = SimpleVectorStore.builder(BagOfWordsEmbeddingModel).build()

    private companion object {
        const val SESSION_A = "session-a"
        const val SESSION_B = "session-b"
    }
}
