package com.ntgjvmagent.orchestrator.entity.agent.knowledge

import com.ntgjvmagent.orchestrator.entity.base.SoftDeletableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "knowledge_files")
class KnowledgeFile(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "knowledge_id")
    val knowledge: AgentKnowledge,
    @Column(nullable = false)
    val fileName: String,
    @Column(nullable = false)
    val filePath: String,
    @Column(nullable = false)
    val contentType: String,
    @Column(nullable = false)
    val fileSize: Long,
) : SoftDeletableEntity()
