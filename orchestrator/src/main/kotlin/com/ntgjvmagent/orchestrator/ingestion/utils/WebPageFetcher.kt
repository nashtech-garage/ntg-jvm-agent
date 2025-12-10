package com.ntgjvmagent.orchestrator.ingestion.utils

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.io.IOException
import java.time.Duration
import java.util.concurrent.TimeoutException

@Component
class WebPageFetcher(
    @Qualifier("crawlerWebClient")
    private val client: WebClient,
) {
    companion object {
        private const val MAX_LOGICAL_BYTES = 2 * 1024 * 1024 // 2MB ingestion limit
        private const val TIMEOUT_SECONDS = 10L
    }

    fun fetch(url: String): String =
        try {
            val responseEntity =
                client
                    .get()
                    .uri(url)
                    .accept(MediaType.TEXT_HTML)
                    .retrieve()
                    .onStatus({ !it.is2xxSuccessful }) { resp ->
                        resp.bodyToMono(String::class.java).map { body ->
                            WebPageFetchException(
                                "HTTP ${resp.statusCode()} for $url: $body",
                            )
                        }
                    }.toEntity(ByteArray::class.java)
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .block()
                    ?: throw WebPageFetchException("Empty response from $url")

            val contentType = responseEntity.headers.contentType
            check(
                contentType != null &&
                    (
                        contentType.includes(MediaType.TEXT_HTML) ||
                            contentType.includes(MediaType.APPLICATION_XHTML_XML)
                    ),
            ) {
                "Unsupported content-type: $contentType for $url"
            }

            val bodyBytes = responseEntity.body ?: ByteArray(0)

            check(bodyBytes.size <= MAX_LOGICAL_BYTES) {
                "Page too large (${bodyBytes.size} bytes). Max allowed = $MAX_LOGICAL_BYTES"
            }

            bodyBytes.toString(Charsets.UTF_8)
        } catch (ex: TimeoutException) {
            throw WebPageFetchException(
                "Timeout (${TIMEOUT_SECONDS}s) when fetching $url",
                ex,
            )
        } catch (ex: WebClientResponseException) {
            throw WebPageFetchException(
                "HTTP ${ex.statusCode} while fetching $url: ${ex.responseBodyAsString}",
                ex,
            )
        } catch (ex: IOException) {
            throw WebPageFetchException(
                "I/O error while fetching $url: ${ex.message}",
                ex,
            )
        } catch (ex: IllegalStateException) {
            throw WebPageFetchException(
                "Invalid state while fetching $url: ${ex.message}",
                ex,
            )
        } catch (ex: IllegalArgumentException) {
            throw WebPageFetchException(
                "Invalid argument while fetching $url: ${ex.message}",
                ex,
            )
        }
}

/**
 * Custom checked-like runtime exception for fetch errors.
 */
class WebPageFetchException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
