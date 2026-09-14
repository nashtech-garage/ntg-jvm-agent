package com.ntgjvmagent.orchestrator.entity.memory

import com.ntgjvmagent.orchestrator.model.MemoryType
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class MemoryTypeConverter : AttributeConverter<MemoryType, String> {
    override fun convertToDatabaseColumn(attribute: MemoryType?): String? = attribute?.value

    override fun convertToEntityAttribute(dbData: String?): MemoryType? = dbData?.let(MemoryType::fromValue)
}
