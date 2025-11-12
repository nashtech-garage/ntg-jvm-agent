package com.ntgjvmagent.orchestrator.entity.base

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.SQLRestriction
import java.time.OffsetDateTime

@MappedSuperclass
@DynamicUpdate
@SQLRestriction("deleted_at IS NULL")
abstract class SoftDeletableEntity : BaseEntity() {
    @Column(name = "deleted_at")
    var deletedAt: OffsetDateTime? = null

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    fun markDeleted() {
        deletedAt = OffsetDateTime.now()
        active = false
    }
}
