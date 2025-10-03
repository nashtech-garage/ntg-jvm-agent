package com.ntgjvmagent.orchestrator.entity

import jakarta.persistence.Column
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import java.time.ZonedDateTime
import java.util.UUID

@MappedSuperclass
abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: UUID? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: ZonedDateTime

    @Column(name = "updated_at")
    var updatedAt: ZonedDateTime? = null

    @PrePersist
    fun onCreate() {
        createdAt = ZonedDateTime.now()
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = ZonedDateTime.now()
    }
}
