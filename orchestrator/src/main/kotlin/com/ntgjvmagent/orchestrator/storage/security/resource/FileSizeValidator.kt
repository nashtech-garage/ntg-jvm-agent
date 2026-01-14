package com.ntgjvmagent.orchestrator.storage.security.resource

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class FileSizeValidator(
    @Value($$"${storage.max-object-size-bytes}")
    private val maxSize: Long,
) {
    fun validate(size: Long) {
        require(size <= maxSize) {
            "File size $size exceeds max allowed $maxSize bytes"
        }
    }
}
