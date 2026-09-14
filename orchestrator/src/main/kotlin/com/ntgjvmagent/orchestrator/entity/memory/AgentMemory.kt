package com.ntgjvmagent.orchestrator.entity.memory

import com.ntgjvmagent.orchestrator.entity.base.SoftDeletableEntity
import com.ntgjvmagent.orchestrator.model.MemoryType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.util.UUID

@Entity
@Table(name = "agent_memory")
@Suppress("LongParameterList")
class AgentMemory(
    @Column(name = "user_id", nullable = false)
    val userId: UUID,
    @Column(name = "agent_id")
    val agentId: UUID?,
    @Convert(converter = MemoryTypeConverter::class)
    @Column(nullable = false, length = 20)
    var type: MemoryType,
    @Column(nullable = false, length = 100)
    var name: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,
    @Version
    @Column(nullable = false)
    var version: Int = 0,
) : SoftDeletableEntity()
