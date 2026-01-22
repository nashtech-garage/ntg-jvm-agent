package com.ntgjvmagent.orchestrator.config.converter

import com.ntgjvmagent.orchestrator.model.ProviderType
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class ProviderTypeConverter : Converter<String, ProviderType> {
    override fun convert(source: String): ProviderType =
        ProviderType.entries.firstOrNull {
            it.name.equals(source.trim(), ignoreCase = true)
        } ?: throw IllegalArgumentException(
            "Unsupported provider type '$source'. " +
                "Supported values: ${ProviderType.entries.joinToString { it.name }}",
        )
}
