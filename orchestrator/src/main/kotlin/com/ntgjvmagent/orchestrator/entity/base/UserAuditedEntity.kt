package com.ntgjvmagent.orchestrator.entity.base

import com.ntgjvmagent.orchestrator.entity.User
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy

@MappedSuperclass
abstract class UserAuditedEntity : BaseEntity() {
    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", updatable = false)
    var createdBy: User? = null

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    var updatedBy: User? = null
}
