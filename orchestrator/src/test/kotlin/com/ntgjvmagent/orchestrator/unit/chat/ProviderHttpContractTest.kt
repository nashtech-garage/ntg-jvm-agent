package com.ntgjvmagent.orchestrator.unit.chat

import com.ntgjvmagent.orchestrator.chat.handlers.AzureOpenAiChatModelHandler
import com.ntgjvmagent.orchestrator.chat.handlers.OpenAiChatModelHandler
import com.ntgjvmagent.orchestrator.config.LlmProvidersProperties
import com.ntgjvmagent.orchestrator.config.ProviderConfig
import com.ntgjvmagent.orchestrator.embedding.config.EmbeddingModelConfig
import com.ntgjvmagent.orchestrator.embedding.handlers.OpenAiEmbeddingModelHandler
import com.ntgjvmagent.orchestrator.model.ChatModelConfig
import com.ntgjvmagent.orchestrator.model.ProviderType
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.micrometer.observation.ObservationRegistry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.prompt.Prompt
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.CopyOnWriteArrayList

class ProviderHttpContractTest {
    private lateinit var server: HttpServer
    private val requests = CopyOnWriteArrayList<CapturedRequest>()

    @BeforeEach
    fun startServer() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/", ::handleRequest)
        server.start()
    }

    @AfterEach
    fun stopServer() {
        server.stop(0)
    }

    @Test
    fun `OpenAI call and stream use configured path and decode SDK payloads`() {
        val model =
            OpenAiChatModelHandler(ObservationRegistry.NOOP).createChatModel(
                chatConfig(
                    provider = ProviderType.OPENAI,
                    baseUrl = baseUrl,
                    path = "/v1/chat/completions",
                    model = "contract-chat",
                ),
            )

        val callText =
            model
                .call(Prompt("hello"))
                .result
                ?.output
                ?.text
        val streamText =
            model
                .stream(Prompt("hello"))
                .mapNotNull { it.result?.output?.text }
                .collectList()
                .block()
                .orEmpty()
                .joinToString("")

        assertEquals("contract response", callText)
        assertEquals("stream response", streamText)
        assertEquals(listOf("/v1/chat/completions", "/v1/chat/completions"), requests.map { it.path })
        assertTrue(requests.all { it.body.contains("contract-chat") })
        assertTrue(requests.all { it.authorization == "Bearer test-key" })
    }

    @Test
    fun `OpenAI embedding uses configured path and decodes SDK payload`() {
        val model =
            OpenAiEmbeddingModelHandler(
                LlmProvidersProperties(
                    mapOf(
                        ProviderType.OPENAI to
                            ProviderConfig(
                                baseUrl = baseUrl,
                                apiKey = "test-key",
                                embeddingsPath = "/v1/embeddings",
                            ),
                    ),
                ),
                ObservationRegistry.NOOP,
            ).createEmbeddingModel(EmbeddingModelConfig(ProviderType.OPENAI, "contract-embedding"))

        assertTrue(model.embed("hello").contentEquals(floatArrayOf(0.1f, 0.2f)))
        assertEquals("/v1/embeddings", requests.single().path)
        assertTrue(requests.single().body.contains("contract-embedding"))
    }

    @Test
    fun `Azure unified client uses deployment route and api key header`() {
        val model =
            AzureOpenAiChatModelHandler(ObservationRegistry.NOOP).createChatModel(
                chatConfig(
                    provider = ProviderType.AZURE_OPENAI,
                    baseUrl = baseUrl,
                    path = "/unused",
                    model = "azure-deployment",
                ),
            )

        assertEquals(
            "contract response",
            model
                .call(Prompt("hello"))
                .result
                ?.output
                ?.text,
        )

        val request = requests.single()
        assertEquals("/openai/deployments/azure-deployment/chat/completions", request.path)
        assertTrue(request.query.orEmpty().contains("api-version="))
        assertEquals("test-key", request.apiKey)
    }

    private val baseUrl: String
        get() = "http://127.0.0.1:${server.address.port}"

    private fun chatConfig(
        provider: ProviderType,
        baseUrl: String,
        path: String,
        model: String,
    ) = ChatModelConfig(
        providerType = provider,
        baseUrl = baseUrl,
        apiKey = "test-key",
        chatCompletionsPath = path,
        modelName = model,
    )

    private fun handleRequest(exchange: HttpExchange) {
        val body = exchange.requestBody.use { String(it.readAllBytes(), StandardCharsets.UTF_8) }
        requests +=
            CapturedRequest(
                path = exchange.requestURI.path,
                query = exchange.requestURI.query,
                body = body,
                authorization = exchange.requestHeaders.getFirst("Authorization"),
                apiKey = exchange.requestHeaders.getFirst("api-key"),
            )

        when {
            exchange.requestURI.path.endsWith("/embeddings") -> {
                respondJson(
                    exchange,
                    """{"object":"list","data":[{"object":"embedding","embedding":[0.1,0.2],"index":0}],"model":"contract-embedding","usage":{"prompt_tokens":1,"total_tokens":1}}""",
                )
            }

            exchange.requestHeaders.getFirst("Accept")?.contains("text/event-stream") == true -> {
                respondEventStream(
                    exchange,
                    listOf(
                        chatChunk("stream ", null),
                        chatChunk("response", "stop"),
                    ),
                )
            }

            else -> {
                respondJson(exchange, CHAT_RESPONSE)
            }
        }
    }

    private fun respondJson(
        exchange: HttpExchange,
        body: String,
    ) {
        exchange.responseHeaders.add("Content-Type", "application/json")
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun respondEventStream(
        exchange: HttpExchange,
        chunks: List<String>,
    ) {
        exchange.responseHeaders.add("Content-Type", "text/event-stream")
        val body = chunks.joinToString("") { "data: $it\n\n" } + "data: [DONE]\n\n"
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun chatChunk(
        content: String,
        finishReason: String?,
    ): String =
        """{"id":"chatcmpl-stream","object":"chat.completion.chunk","created":1,"model":"contract-chat","choices":[{"index":0,"delta":{"role":"assistant","content":"$content"},"finish_reason":${finishReason?.let {
            "\"$it\""
        } ?: "null"}}]}"""

    private data class CapturedRequest(
        val path: String,
        val query: String?,
        val body: String,
        val authorization: String?,
        val apiKey: String?,
    )

    companion object {
        private const val CHAT_RESPONSE =
            """{"id":"chatcmpl-test","object":"chat.completion","created":1,"model":"contract-chat","choices":[{"index":0,"message":{"role":"assistant","content":"contract response"},"finish_reason":"stop"}],"usage":{"prompt_tokens":1,"completion_tokens":1,"total_tokens":2}}"""
    }
}
