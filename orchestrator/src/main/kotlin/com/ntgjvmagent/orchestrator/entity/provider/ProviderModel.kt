package com.ntgjvmagent.orchestrator.entity.provider

import com.ntgjvmagent.orchestrator.entity.base.SoftDeletableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal

@Entity
@Table(
    name = "provider_model",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_provider_model_name", columnNames = ["provider_id", "model_name"]),
    ],
)
data class ProviderModel(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    var provider: Provider,
    @Column(name = "model_name", nullable = false, length = 150)
    var modelName: String,
    @Column(nullable = false, length = 30)
    var type: String, // "chat" or "completion"
    @Column(name = "context_window")
    var contextWindow: Int? = null,
    // Defaults
    @Column(name = "default_temperature", precision = 3, scale = 2)
    var defaultTemperature: BigDecimal = BigDecimal("0.70"),
    @Column(name = "default_top_p", precision = 3, scale = 2)
    var defaultTopP: BigDecimal = BigDecimal("1.00"),
    @Column(name = "default_max_tokens")
    var defaultMaxTokens: Int = 2048,
    @Column(name = "default_frequency_penalty", precision = 3, scale = 2)
    var defaultFrequencyPenalty: BigDecimal = BigDecimal("0.00"),
    @Column(name = "default_presence_penalty", precision = 3, scale = 2)
    var defaultPresencePenalty: BigDecimal = BigDecimal("0.00"),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_embedding_model_id")
    var defaultEmbeddingModel: ProviderEmbeddingModel? = null,
    @Column(name = "default_dimension")
    var defaultDimension: Int = 1536,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", columnDefinition = "jsonb")
    var defaultSettings: Map<String, Any>? = null,
) : SoftDeletableEntity()
