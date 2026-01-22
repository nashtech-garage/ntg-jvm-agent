package com.ntgjvmagent.orchestrator.entity.provider

import com.ntgjvmagent.orchestrator.entity.base.SoftDeletableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "provider_embedding_model")
data class ProviderEmbeddingModel(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    var provider: Provider,
    @Column(name = "embedding_name", nullable = false, length = 150)
    var embeddingName: String,
    @Column(nullable = false)
    var dimension: Int,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var settings: Map<String, Any>? = null,
) : SoftDeletableEntity()
