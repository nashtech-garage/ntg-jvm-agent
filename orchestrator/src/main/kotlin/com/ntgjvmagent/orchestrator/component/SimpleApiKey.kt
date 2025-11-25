package com.ntgjvmagent.orchestrator.component

import org.springframework.ai.model.ApiKey

class SimpleApiKey(
    private val key: String,
) : ApiKey {
    override fun getValue(): String = key
}
