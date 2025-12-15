package com.ntgjvmagent.orchestrator.ingestion.utils

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
class WebPageFetcher(
    @Qualifier("crawlerRestClient")
    private val rest: RestClient,
    private val urlSafetyValidator: UrlSafetyValidator,
) {
    companion object {
        const val MAX_PAGE_SIZE_BYTES = 2 * 1024 * 1024 // 2MB
    }

    fun fetch(url: String): String {
        urlSafetyValidator.validate(url)

        return try {
            val response =
                rest
                    .get()
                    .uri(url)
                    .retrieve()
                    .toEntity(ByteArray::class.java)

            validateContentType(url, response.headers.contentType)

            val body = response.body ?: ByteArray(0)
            validateSize(url, body)

            body.toString(Charsets.UTF_8)
        } catch (ex: RestClientException) {
            throw WebPageFetchException("HTTP error while fetching $url: ${ex.message}", ex)
        } catch (ex: IllegalArgumentException) {
            throw WebPageFetchException("Invalid request to $url: ${ex.message}", ex)
        } catch (ex: IllegalStateException) {
            throw WebPageFetchException("Client error while fetching $url: ${ex.message}", ex)
        }
    }

    private fun validateContentType(
        url: String,
        contentType: MediaType?,
    ) {
        val ok =
            contentType != null &&
                (
                    contentType.includes(MediaType.TEXT_HTML) ||
                        contentType.includes(MediaType.APPLICATION_XHTML_XML)
                )

        if (!ok) {
            throw WebPageFetchException("Unsupported content-type for $url: $contentType")
        }
    }

    private fun validateSize(
        url: String,
        body: ByteArray,
    ) {
        if (body.size > MAX_PAGE_SIZE_BYTES) {
            throw WebPageFetchException(
                "Page too large for $url: ${body.size} bytes (max=$MAX_PAGE_SIZE_BYTES)",
            )
        }
    }
}

class WebPageFetchException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
