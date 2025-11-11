package com.ntgjvmagent.mcpserver.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.ntgjvmagent.mcpserver.utils.Constant
import com.ntgjvmagent.mcpserver.viewmodel.GoogleSearchResponseVm
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.messages.ToolResponseMessage
import org.springframework.ai.tool.annotation.Tool
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.util.UUID

@Service
class SearchOnlineTool(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(SearchOnlineTool::class.java)

    @Value("\${google.api.key}")
    private lateinit var apiKey: String

    @Value("\${google.search.engine.id}")
    private lateinit var searchEngineID: String

    @Value("\${google.search.endpoint}")
    private lateinit var endpoint: String

    @Value("\${google.search.sort}")
    private lateinit var sortParam: String

    @Tool(
        description = """
        Search the web for up-to-date information.
        ALWAYS use this tool when the user asks about current events, real-time data, or information like prices, weather, news, etc.
    """,
    )
    fun searchOnline(query: String): ToolResponseMessage.ToolResponse {
        val resultToolName = "Result of searchOnline tool"
        val results = callGoogleSearchAPI(query)
        if (results.isEmpty()) {
            return ToolResponseMessage.ToolResponse(
                UUID.randomUUID().toString(),
                resultToolName,
                "No relevant results found.",
            )
        }

        val summary =
            results.joinToString("\n\n") { item ->
                val title = item.title ?: "No title"
                val snippet = item.snippet ?: "No snippet"
                val link = item.link ?: ""
                "**$title**\n$snippet\n$link"
            }

        val formattedAnswer =
            buildString {
                append("Search results for \"$query\":\n\n")
                append(summary)
                append("\n\n(Source: Google Search)")
            }

        return ToolResponseMessage.ToolResponse(
            UUID.randomUUID().toString(),
            resultToolName,
            formattedAnswer,
        )
    }

    fun callGoogleSearchAPI(query: String): List<GoogleSearchResponseVm> {
        val builder =
            UriComponentsBuilder
                .fromUriString(endpoint)
                .queryParam("key", apiKey)
                .queryParam("cx", searchEngineID)
                .queryParam("q", query)
                .queryParam("num", Constant.NUMBER_RESULT_TAKEN)
        if (sortParam.isNotBlank()) {
            builder.queryParam("sort", sortParam)
        }
        val uri = builder.build().toUri()
        return try {
            val response = restTemplate.getForObject(uri, Map::class.java)
            val items = response?.get("items")
            if (items != null) {
                objectMapper.convertValue(items, object : TypeReference<List<GoogleSearchResponseVm>>() {})
            } else {
                emptyList()
            }
        } catch (ex: HttpClientErrorException) {
            logger.error("HTTP error from Google API: ${ex.statusCode} - ${ex.responseBodyAsString}", ex)
            emptyList()
        } catch (ex: ResourceAccessException) {
            logger.error("Network error when calling Google API", ex)
            emptyList()
        } catch (ex: IllegalArgumentException) {
            logger.error("Unexpected error while jackson converting", ex)
            emptyList()
        }
    }
}
