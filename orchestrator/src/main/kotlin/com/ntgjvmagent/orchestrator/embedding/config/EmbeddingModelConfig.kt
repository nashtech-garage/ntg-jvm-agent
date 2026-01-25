package com.ntgjvmagent.orchestrator.embedding.config

import com.ntgjvmagent.orchestrator.config.EmbeddingProperties
import com.ntgjvmagent.orchestrator.model.ProviderType

data class EmbeddingModelConfig(
    val providerType: ProviderType,
    val model: String,
) {
    companion object {
        /**
         * Build embedding config from application-level embedding properties.
         * This is GLOBAL, not agent-based.
         */
        fun fromEmbeddingProperties(props: EmbeddingProperties): EmbeddingModelConfig =
            EmbeddingModelConfig(
                providerType = props.provider,
                model = props.model,
            )
    }
}
