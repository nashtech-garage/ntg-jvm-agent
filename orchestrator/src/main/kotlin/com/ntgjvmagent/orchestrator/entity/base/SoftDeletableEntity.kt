package com.ntgjvmagent.orchestrator.entity.base

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.SQLRestriction
import java.time.Instant

@MappedSuperclass
@DynamicUpdate
@SQLRestriction("deleted_at IS NULL")
abstract class SoftDeletableEntity : UserAuditedEntity() {
    @Column(name = "deleted_at")
    var deletedAt: Instant? = null

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    fun markDeleted() {
        deletedAt = Instant.now()
        active = false
    }
}
