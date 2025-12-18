package com.ntgjvmagent.orchestrator.entity.provider

import com.ntgjvmagent.orchestrator.entity.base.SoftDeletableEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "provider")
data class Provider(
    @Column(nullable = false, length = 100)
    var name: String,
    @Column(columnDefinition = "TEXT")
    var description: String? = null,
    @Column(name = "base_url", nullable = false, length = 200)
    var baseUrl: String,
    @Column(name = "chat_completions_path", length = 100, nullable = false)
    var chatCompletionsPath: String = "/v1/chat/completions",
    @Column(name = "embeddings_path", length = 100, nullable = false)
    var embeddingsPath: String = "/v1/embeddings",
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var settings: Map<String, Any>? = null,
) : SoftDeletableEntity() {
    @OneToMany(mappedBy = "provider", cascade = [CascadeType.ALL], orphanRemoval = true)
    var models: MutableSet<ProviderModel> = mutableSetOf()

    @OneToMany(mappedBy = "provider", cascade = [CascadeType.ALL], orphanRemoval = true)
    var embeddingModels: MutableSet<ProviderEmbeddingModel> = mutableSetOf()
}
