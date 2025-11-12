package com.ntgjvmagent.orchestrator.entity.base.listener

import com.ntgjvmagent.orchestrator.entity.base.SoftDeletableEntity
import jakarta.persistence.PreRemove
import java.time.OffsetDateTime

object SoftDeleteListener {
    @PreRemove
    fun onPreRemove(entity: Any) {
        if (entity is SoftDeletableEntity) {
            entity.deletedAt = OffsetDateTime.now()
            entity.active = false
        }
    }
}
