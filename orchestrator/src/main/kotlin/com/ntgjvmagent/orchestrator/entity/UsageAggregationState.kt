package com.ntgjvmagent.orchestrator.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "usage_aggregation_state")
class UsageAggregationState(
    @Id
    @Column(nullable = false)
    val id: Short = 1,
    @Column(name = "last_processed_date", nullable = false)
    var lastProcessedDate: LocalDate,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
